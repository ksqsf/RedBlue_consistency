package applications.microbenchmark.SimpleTest;


import util.Debug;

import txstore.util.ProxyTxnId;
import txstore.proxy.OpenLoopProxyInterface;
import txstore.proxy.SimpleOpenLoopProxy;
import txstore.proxy.OpenLoopApplicationInterface;

import txstore.util.Operation;
import txstore.util.Result;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

import java.util.Hashtable;

import applications.microbenchmark.FakedScratchpadTest.FakedWorkloadGenerator;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;
import applications.simplestub.OperationList;

public class SimpleProxy implements OpenLoopApplicationInterface {

	int dcId;
	int proxyId;
	int objectCount;
	int keySelection;
	double blueRatio;
	int readReqNum;
	int writeReqNum;
    SimpleAppServer fas;
    Hashtable<ProxyTxnId, TxnReqMessage> idMappings = new Hashtable<ProxyTxnId, TxnReqMessage>(); 
    FakedWorkloadGenerator fwl;
    SimpleOpenLoopProxy proxy;

    public SimpleProxy(int dcId, int proxyId, double bR, int r, int w, int objC, int keySel){
		this.dcId = dcId;
		this.proxyId = proxyId;
		this.blueRatio = bR;
		this.readReqNum = r;
		this.writeReqNum = w;
		this.objectCount = objC;
		int min_key = 0;
		int max_key = objectCount - 1;
      fwl = new FakedWorkloadGenerator(blueRatio, r, w, min_key, max_key, keySelection);
      idMappings = new Hashtable<ProxyTxnId, TxnReqMessage>();
    }

    public void setProxy(SimpleOpenLoopProxy p) {
        proxy = p;
    }
    
    public void setAppServer(SimpleAppServer f){
    	fas = f;
    }

    public int selectStorageServer(Operation op) {
        return 0;
    }

    public int selectStorageServer(byte[] op) {
        return 0;
    }

    public void returnResult(ProxyTxnId txnid,
            byte[] res) {
        proxy.commit(txnid);
    }

    public void transactionStarted(ProxyTxnId txnid) {

    	TxnReqMessage msg = idMappings.get(txnid);
    	int userId = msg.getTxnId().getProxyId();
		int dcId = msg.getTxnId().getDcId();
		Debug.printf("this transaction is from dcId %d user %d\n", dcId, userId);
		OperationList opList = fwl.generate_commands(userId*10 + dcId, msg.isRead());
        byte[] tmp = new byte[opList.getByteSize()];
        opList.getBytes(tmp, 0);
        proxy.execute(tmp, txnid);

    }
    long abortcount = 0;
    Object abort = new Object();

    public void transactionAborted(ProxyTxnId id) {
    	
    	//notify user
    	TxnReqMessage userMsg = idMappings.get(id);
    	TxnRepMessage msg = new TxnRepMessage(userMsg.getTxnId(), 1, 0);
    	fas.replyToUser(msg);
    	idMappings.remove(id);
    }
    
    public void transactionCommitted(ProxyTxnId id) {
    	
    	//notify user
    	TxnReqMessage userMsg = idMappings.get(id);
    	TxnRepMessage msg = new TxnRepMessage(userMsg.getTxnId(), 0, 0);
    	fas.replyToUser(msg);
    	idMappings.remove(id);
    }
    Object starting = new Object();

    public void beginTransaction(TxnReqMessage msg) {
       ProxyTxnId pid = proxy.beginTxn();
       idMappings.put(pid, msg);
       transactionStarted(pid);

    }
    static public long startTime = 0;

    public static void main(String arg[]) {
    	if (arg.length != 12) {
			System.out
					.println("usage: SimpleProxy dc_config.xml dc_user_config.xml dcId proxyId aSthdcount proxythdcount tcpnodelay bluerate readReqNum writeReqNum objectCount keySelection");
			System.exit(-1);
		}
        SimpleProxy olsp = new SimpleProxy(Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), Double.parseDouble(arg[7]), Integer.parseInt(arg[8]), Integer.parseInt(arg[9]), Integer.parseInt(arg[10]), Integer.parseInt(arg[11]));
        SimpleOpenLoopProxy imp = new SimpleOpenLoopProxy(arg[0], Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), olsp);
	olsp.setProxy(imp);


		Boolean tcpnodelay = Boolean.parseBoolean(arg[6]);
		// set up the networking for outgoing messages
		NettyTCPSender sendNet = new NettyTCPSender();
		imp.setSender(sendNet);
		sendNet.setTCPNoDelay(tcpnodelay);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the proxy
                int proxythdcount = Integer.parseInt(arg[5]);
                ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, proxythdcount);
		// then create the actual network
		
		NettyTCPReceiver rcv = new NettyTCPReceiver(
				imp.getMembership().getMe().getInetSocketAddress(), ptnq, 2);

		// set up an appServer

		SimpleAppServer fas = new SimpleAppServer(arg[1], Integer
				.parseInt(arg[2]), Integer.parseInt(arg[3]), olsp);

		// set up the networking for outgoing messages
		NettyTCPSender sendNet1 = new NettyTCPSender();
		fas.setSender(sendNet1);
		sendNet1.setTCPNoDelay(tcpnodelay);
		
		olsp.setAppServer(fas);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the appServer
                int aSthdcount = Integer.parseInt(arg[4]);
		ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(fas, aSthdcount);
		// then create the actual network
		
		NettyTCPReceiver rcv1 = new NettyTCPReceiver(fas.getMembership()
				.getMe().getInetSocketAddress(), ptnq1, 2);


    }
}
