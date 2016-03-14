package applications.microbenchmark.ScratchpadTest;
import util.Debug;

import network.ParallelPassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;
import txstore.proxy.ApplicationInterface;
import txstore.util.Operation;
import applications.microbenchmark.util.Databases;
import applications.microbenchmark.util.MicroAppServer;
import applications.microbenchmark.util.MicroWorkloadGenerator;
import applications.microbenchmark.util.TestName;

public class ScratchPadProxy implements ApplicationInterface {
	/*The following two parameters for workload generator*/
	
	static MicroAppServer mas;
	
	public Databases dbs;
	public static MicroWorkloadGenerator mwl;
	int dcId;
	int proxyId;
	
	public ScratchPadProxy(String dbConfFile, int dcId, double bR, int r, int w, int obC, int keySel) {
		dbs = new Databases();
		dbs.parseXMLfile(dbConfFile);
		dbs.printOut();
		mwl = new MicroWorkloadGenerator(dbs.dbList, dcId, bR, r,w, 0, obC - 1, keySel);
		
	}
	
	public MicroWorkloadGenerator getWorkloadGenerator(){
		return mwl;
	}

	public int selectStorageServer(Operation op) {
		String newop = new String(op.getOperation());
		int i_server = mwl.select_server(newop);
		return i_server;
	}

	public int selectStorageServer(byte[] op) {
		String newop = new String(op);
		int i_server = mwl.select_server(newop);
		return i_server;
	}
	
	public static void main(String arg[]) {
		if (arg.length != 12) {
			System.out
					.println("usage: ScratchPadProxy dc_user_config.xml dcId proxyId db_config.xml threadcount tcpdelay  bluerate readReqNum writeReqNum  objectCount keySelection connNum");
			System.exit(-1);
		}
		
		ScratchPadProxy sp = new ScratchPadProxy(arg[3],Integer.parseInt(arg[1]), Double.parseDouble(arg[6]), Integer.parseInt(arg[7]), Integer.parseInt(arg[8]), Integer.parseInt(arg[9]), Integer.parseInt(arg[10]));
		
		// set up an appServer
		mas = new MicroAppServer(arg[0], Integer
				.parseInt(arg[1]), Integer.parseInt(arg[2]), sp, TestName.SCRATCHPADPROXY, Integer.parseInt(arg[11]));

		// set up the networking for outgoing messages
		NettyTCPSender sendNet1 = new NettyTCPSender();
		mas.setSender(sendNet1);
		boolean tcpnodelay = Boolean.parseBoolean(arg[5]);
		sendNet1.setTCPNoDelay(tcpnodelay);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the appServer
                int threadcount = Integer.parseInt(arg[4]);
		ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(mas, threadcount);
		// then create the actual network
		
		NettyTCPReceiver rcv1 = new NettyTCPReceiver(mas.getMembership()
				.getMe().getInetSocketAddress(), ptnq1, 2);
	}
}

