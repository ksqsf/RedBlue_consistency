package applications.microbenchmark.MemoryKVScratchpadTest;
import util.Debug;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import txstore.proxy.ClosedLoopProxy;
import txstore.proxy.Proxy;
import txstore.util.ProxyTxnId;

import applications.microbenchmark.messages.MessageBase;
import applications.microbenchmark.messages.MessageFactory;
import applications.microbenchmark.messages.MessageTags;
import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.MicroBaseNode;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;
import applications.simplestub.OperationList;

public class FakedMemKVAppServer extends MicroBaseNode {

	int dcId;
	int appServerId;
	MessageFactory mf;
	FakedMemKVProxy fp;
	ClosedLoopProxy imp;
	FakedMemKVWorkloadGenerator fwl;

	public FakedMemKVAppServer(String file_name, int s1, int s2, FakedMemKVProxy f,
			ClosedLoopProxy imp2) {
		super(file_name, s1, Role.APPSERVER, s2);
		dcId = s1;
		appServerId = s2;
		fp = f;
		imp = imp2;
		mf = new MessageFactory();
		fwl = fp.getWorkloadGenerator();
	}

	public void issueTxn(TxnReqMessage msg) {
		// generate commands here
		boolean success = false;
		int userId = msg.getTxnId().getProxyId();
		int dcId = msg.getTxnId().getDcId();
		Debug.printf("this transaction is from dcId %d, user %d\n",dcId, userId);
		OperationList opList = fwl.generate_commands(userId*10 + dcId, msg.isRead());

		byte[] tmp = new byte[45];
		while (!success) {
			ProxyTxnId txnId = imp.beginTxn();
			tmp = new byte[opList.getByteSize()];
			opList.getBytes(tmp, 0);
			byte[] res = imp.execute(tmp, txnId);
			for (int j = 0; j < res.length; j++)
				if (res[j] != tmp[j])
					throw new RuntimeException(
							"error! byte arrays dont match!");
			if (res.length != tmp.length)
				throw new RuntimeException(
						"error!  byte array lengths are bad");
			success = imp.commit(txnId);
			if (!success)
				Debug.println("\t\ttransaction: " + txnId
						+ " was aborted");
		}
		Debug.println("txn succeeded \n");
		TxnRepMessage msg1 = new TxnRepMessage(msg.getTxnId(), 0, opList.getColor());
		replyToUser(dcId, userId, msg1);	
	}

	@Override
	public void handle(byte[] b) {
		// TODO Auto-generated method stub
		MessageBase msg = mf.fromBytes(b);
		Debug.println(msg);
		if (msg == null)
			throw new RuntimeException("Should never receive a null message");

		if (msg.getTag() == MessageTags.TXNREQ) {
			process((TxnReqMessage) msg);

		} else {
			throw new RuntimeException("invalid message tag: " + msg.getTag());
		}
	}

	private void process(TxnReqMessage msg) {
		// TODO Auto-generated method stub
		issueTxn(msg);
	}

	public void replyToUser(int dcId, int userId, TxnRepMessage msg) {
		sendToUser(msg, dcId, userId);
	}

}
