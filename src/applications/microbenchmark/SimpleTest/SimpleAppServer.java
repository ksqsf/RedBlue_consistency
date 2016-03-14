package applications.microbenchmark.SimpleTest;
import util.Debug;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import txstore.proxy.SimpleCloseLoopProxy;
import txstore.proxy.SimpleOpenLoopProxy;
import txstore.util.ProxyTxnId;

import applications.microbenchmark.FakedScratchpadTest.FakedWorkloadGenerator;
import applications.microbenchmark.messages.MessageBase;
import applications.microbenchmark.messages.MessageFactory;
import applications.microbenchmark.messages.MessageTags;
import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.MicroBaseNode;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;
import applications.simplestub.OperationList;

public class SimpleAppServer extends MicroBaseNode {

	int dcId;
	int appServerId;
	MessageFactory mf;
	SimpleProxy imp;

	public SimpleAppServer(String file_name, int s1, int s2, SimpleProxy f) {
		super(file_name, s1, Role.APPSERVER, s2);
		dcId = s1;
		appServerId = s2;
		imp = f;
		mf = new MessageFactory();
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
		//call begin transacton
		imp.beginTransaction(msg);
		
	}

	public void replyToUser(TxnRepMessage msg) {
		int dataCenterId = msg.getTxnId().getDcId();
		int userId = msg.getTxnId().getProxyId();
		sendToUser(msg, dataCenterId, userId);
	}

}
