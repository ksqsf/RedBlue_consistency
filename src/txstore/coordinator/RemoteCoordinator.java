package txstore.coordinator;

import txstore.BaseNode;
import txstore.membership.Role;

import txstore.messages.MessageFactory;
import txstore.messages.MessageTags;
import txstore.messages.MessageBase;

// receiving messages
import txstore.messages.AckCommitTxnMessage;
import txstore.messages.BeginTxnMessage;
import txstore.messages.BlueTokenGrantMessage;
import txstore.messages.CommitShadowOpMessage;
import txstore.messages.FinishTxnMessage;
import txstore.messages.OperationMessage;
import txstore.messages.ProxyCommitMessage;
import txstore.messages.ReadWriteSetMessage;
import txstore.messages.RemoteShadowOpMessage;
import txstore.messages.TxnReadyMessage;
import txstore.messages.TxnMetaInformationMessage;

// sending messages
import txstore.messages.AckTxnMessage;
import txstore.messages.CommitTxnMessage;
import txstore.messages.AbortTxnMessage;
import txstore.messages.FinishRemoteMessage;
import txstore.messages.GimmeTheBlueMessage;

import txstore.storageshim.StorageShim;
import txstore.util.Operation;
import txstore.util.ProxyTxnId;
import txstore.util.TimeStamp;
import txstore.util.LogicalClock;
import txstore.util.ReadWriteSet;
import txstore.util.StorageList;
import txstore.util.ReadSetEntry;
import txstore.util.ReadSet;
import txstore.util.WriteSet;
import txstore.util.WriteSetEntry;

import util.Counter;
import util.Debug;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;
import network.PassThroughNetworkQueue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
import java.util.Vector;

//for logging
import org.apache.log4j.*;

public class RemoteCoordinator extends BaseNode {

	MessageFactory mf;
	NewCoordinator coord; 
	
	ObjectPool<TransactionRecord> txnPool;
	Hashtable<ProxyTxnId, TransactionRecord> records;

	public RemoteCoordinator(String file, int dc, int id) {
		super(file, dc, Role.REMOTECOORDINATOR, id);
		System.out.println("start remote coordinator acceptor");
		this.mf = new MessageFactory();
		records = new Hashtable<ProxyTxnId, TransactionRecord>();
		
		//initiate the txnPool
		txnPool = new ObjectPool<TransactionRecord>();
		for(int i = 0; i < 100; i++){
			TransactionRecord txn = new TransactionRecord();
			txnPool.addObject(txn);
		}
		Debug.println("RemoteCoordinator acceptor finished initialization and starts");
	}
	
	public void setCoordinator(NewCoordinator c){
		coord = c;
	}

	/***
	 * handle incoming messages. implements ByteHandler
	 ***/
	public void handle(byte[] b) {
		MessageBase msg = mf.fromBytes(b);
		if (msg == null) {
			throw new RuntimeException("Should never receive a null message");
		}
		
		 if (coord.messageCount.incrementAndGet() % 5000 == 0) { 
			 coord.messageCount.set(0);
			 System.out.println("beginTxn  |  gimetheblue |  abortxn | bluetokengrant | proxycommit | ackcommit | remoteshadow ");
			 for (int i = 0; i < coord.messages.length; i++) {
				 System.out.print(coord.messages[i] + "\t"); coord.messages[i] = 0; 
			 }
			 System.out.println();
		 }
		switch (msg.getTag()) {
		case MessageTags.ACKCOMMIT:
			coord.messages[5]++;
			process((AckCommitTxnMessage) msg);
			return;
		case MessageTags.REMOTESHADOW:
			coord.messages[6]++;
			process((RemoteShadowOpMessage) msg);
			return;
		default:
			throw new RuntimeException("invalid message tag: " + msg.getTag());
		}

	}

	private void process(AckCommitTxnMessage msg) {
		// TODO Auto-generated method stub
		Debug.println("receive ack commit " + msg);
		
		TransactionRecord tmpRec = records.get(msg.getTxnId());
		coord.updateLastCommittedLogicalClock(tmpRec.getMergeClock(), tmpRec.isBlue());
		coord.updateLastVisibleLogicalClock(tmpRec.getMergeClock());
		// insure that one dc doesnt "always win" because another is underloaded
		coord.setLocalTxn(tmpRec.getFinishTime().getCount());
		
		coord.updateObjectTable(tmpRec.getWriteSet().getWriteSet(), tmpRec.getMergeClock(), 
				tmpRec.getFinishTime(), tmpRec.getTxnId());
		
		coord.statisticOutput(tmpRec);
		records.remove(tmpRec.getTxnId());
		
		//clean datastructure
		mf.returnRemoteShadowOpMessage(tmpRec.rOpMsg);
		mf.returnCommitShadowOpMessage(tmpRec.cSMsg);
		tmpRec.reset();
		txnPool.returnObject(tmpRec);
		mf.returnAckCommitTxnMessage(msg);
	}

	private void process(RemoteShadowOpMessage msg) {
		// TODO Auto-generated method stub
		Debug.println("receive remote shadow " + msg);
		TransactionRecord txn  = txnPool.borrowObject();
		if(txn == null){
			txn = new TransactionRecord(msg.getTxnId(), msg.getTimeStamp(), msg.getLogicalClock());
		}else{
			txn.setTxnId(msg.getTxnId());
		}
		records.put(msg.getTxnId(), txn);
		txn.setWriteSet(msg.getWset());
		txn.setShadowOp(msg.getShadowOperation());
		txn.setColor(msg.getColor());
		txn.setMergeClock(msg.getLogicalClock());
		txn.setFinishTime(msg.getTimeStamp());
		txn.setRemote();
		txn.addStorage(0);
		txn.setRemoteShadowOpMessage(msg);
		
		CommitShadowOpMessage csm = mf.borrowCommitShadowOpMessage();
		if(csm == null){
			csm = new CommitShadowOpMessage(txn.getTxnId(), 
				txn.getShadowOp(), msg.getTimeStamp(), txn.getMergeClock());
		}else{
			csm.encodeMessage(txn.getTxnId(), 
				txn.getShadowOp(), msg.getTimeStamp(), txn.getMergeClock());
		}
		txn.setCommitShadowOpMessage(csm);
		Debug.println("commit remote to data writer" + csm);
		sendToStorage(csm, 0); //TODO: fix to more generic
	}


}

