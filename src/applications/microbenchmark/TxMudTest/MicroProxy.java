package applications.microbenchmark.TxMudTest;
import util.Debug;

import applications.microbenchmark.util.Databases;
import applications.microbenchmark.util.MicroAppServer;
import applications.microbenchmark.util.MicroWorkloadGenerator;
import applications.microbenchmark.util.TestName;

import txstore.scratchpad.rdbms.jdbc.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import txstore.util.ProxyTxnId;
import txstore.util.Result;
import txstore.membership.Membership;
import txstore.membership.Role;
import txstore.proxy.ApplicationInterface;
import txstore.proxy.ClosedLoopProxy;

import txstore.util.Operation;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

public class MicroProxy implements ApplicationInterface {
	/* The following two parameters for workload generator */
	static MicroAppServer mas;
	public Databases dbs;
	public static MicroWorkloadGenerator mwl;

	public MicroProxy(String dbConfFile, int dcId, double bR, int r, int w, int obC, int keySel) {
		dbs = new Databases();
		dbs.parseXMLfile(dbConfFile);
		dbs.printOut();
		mwl = new MicroWorkloadGenerator(dbs.dbList, dcId, bR, r, w, 0, obC-1, keySel);

	}

	public MicroWorkloadGenerator getMicroWorkloadGenerator(){
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
		if (arg.length != 14) {
			System.out
					.println("usage: MicroProxy dc_config.xml user_config.xml dcId proxyId db_config.xml applicationThreads proxyThreads tcpnodelay bluerate readReqNum writeReqNum objectCount keySelection connNum");
			System.exit(-1);
		}

		Membership mem = new Membership(arg[0], Integer.parseInt(arg[2]),
				Role.PROXY, Integer.parseInt(arg[3]));
		
		MicroProxy mp = new MicroProxy(arg[4], Integer.parseInt(arg[2]),  Double.parseDouble(arg[8]), 
				Integer.parseInt(arg[9]), Integer.parseInt(arg[10]), Integer.parseInt(arg[11]), Integer.parseInt(arg[12]));
		ClosedLoopProxy imp = new ClosedLoopProxy(arg[0], Integer.parseInt(arg[2]), Integer
				.parseInt(arg[3]), mp, new ExecuteScratchpadFactory(mem.getDatacenterCount(), Integer.parseInt(arg[2]), 0, arg[4],Integer.parseInt(arg[13])));
		
		//ClosedLoopProxy imp = new ClosedLoopProxy(arg[0], Integer.parseInt(arg[2]), Integer
				//.parseInt(arg[3]), mp, new ScratchpadFactory(mem.getDatacenterCount(), Integer.parseInt(arg[2]), 0, arg[4],Integer.parseInt(arg[13])));
		int threadcount = Integer.parseInt(arg[5]);
		int proxyThdCount = Integer.parseInt(arg[6]);

		Boolean tcpnodelay = Boolean.parseBoolean(arg[7]);
		
		// set up the networking for outgoing messages
		NettyTCPSender sendNet = new NettyTCPSender();
		imp.setSender(sendNet);
		//sendNet.setTCPNoDelay(tcpnodelay);
		sendNet.setTCPNoDelay(false);
		sendNet.setKeepAlive(true);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the proxy
		ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, 10);
		// then create the actual network
		NettyTCPReceiver rcv = new NettyTCPReceiver(
				imp.getMembership().getMe().getInetSocketAddress(), ptnq, 10);

		// set up an appServer

		mas = new MicroAppServer(arg[1], Integer
				.parseInt(arg[2]), Integer.parseInt(arg[3]), mp, imp, TestName.MICROPROXY, Integer.parseInt(arg[13]));

		// set up the networking for outgoing messages
		NettyTCPSender sendNet1 = new NettyTCPSender();
		mas.setSender(sendNet1);
		//sendNet1.setTCPNoDelay(tcpnodelay);
		sendNet1.setTCPNoDelay(false);
		sendNet1.setKeepAlive(true);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the appServer
		ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(mas, 10);
		// then create the actual network
		NettyTCPReceiver rcv1 = new NettyTCPReceiver(mas.getMembership().getMe().getInetSocketAddress(), ptnq1, 10);
	}
}
