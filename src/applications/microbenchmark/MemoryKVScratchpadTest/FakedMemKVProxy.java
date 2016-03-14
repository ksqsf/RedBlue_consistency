package applications.microbenchmark.MemoryKVScratchpadTest;

import network.ParallelPassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;
import txstore.proxy.ApplicationInterface;
import txstore.proxy.ClosedLoopProxy;
import txstore.util.Operation;

public class FakedMemKVProxy implements ApplicationInterface {
	/*The following two parameters for workload generator*/
	
	static FakedMemKVAppServer fas;
	
	public static FakedMemKVWorkloadGenerator fwl;
	int dcId;
	int proxyId;
	
	
	public FakedMemKVProxy(int dcId, int proxyId, double bR, int r, int w, int objC, int keySel) {
	    this.dcId = dcId;
	    this.proxyId = proxyId;
			fwl = new FakedMemKVWorkloadGenerator(bR, r, w, 0, objC - 1, keySel);
	}
	
	public FakedMemKVWorkloadGenerator getWorkloadGenerator(){
		return fwl;
	}

	public int selectStorageServer(Operation op) {
		return 0;
	}

	public int selectStorageServer(byte[] op) {
		return 0;
	}
	
	public static void main(String arg[]) {
		if (arg.length != 12) {
			System.out
					.println("usage: FakedProxy dc_config.xml dc_user_config.xml dcId proxyId aSthdcount proxythdcount tcpnodelay bluerate readReqNum writeReqNum objectCount keySelection");
			System.exit(-1);
		}
		
		FakedMemKVProxy fP = new FakedMemKVProxy(Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), Double.parseDouble(arg[7]), Integer.parseInt(arg[8]),
				Integer.parseInt(arg[9]), Integer.parseInt(arg[10]), Integer.parseInt(arg[11]));
		//ClosedLoopProxy imp = new ClosedLoopProxy(arg[0], Integer.parseInt(arg[2]), Integer
			//	.parseInt(arg[3]), fP);
		ClosedLoopProxy imp = null;
		
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

		fas = new FakedMemKVAppServer(arg[1], Integer
				.parseInt(arg[2]), Integer.parseInt(arg[3]), fP, imp);

		// set up the networking for outgoing messages
		NettyTCPSender sendNet1 = new NettyTCPSender();
		fas.setSender(sendNet1);
		sendNet1.setTCPNoDelay(tcpnodelay);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the appServer
                int aSthdcount = Integer.parseInt(arg[4]);
		ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(fas, aSthdcount);
		// then create the actual network
		
		NettyTCPReceiver rcv1 = new NettyTCPReceiver(fas.getMembership()
				.getMe().getInetSocketAddress(), ptnq1, 2);
	}
}
