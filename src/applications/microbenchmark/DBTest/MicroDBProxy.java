package applications.microbenchmark.DBTest;
import util.Debug;

import applications.microbenchmark.util.Database;
import applications.microbenchmark.util.Databases;
import applications.microbenchmark.util.MicroDBAppServer;
import applications.microbenchmark.util.MicroWorkloadGenerator;
import applications.microbenchmark.util.TestName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import network.ParallelPassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;

import txstore.proxy.ApplicationInterface;

import txstore.util.Operation;

public class MicroDBProxy implements ApplicationInterface {
	/*The following two parameters for workload generator*/
	
	static MicroDBAppServer mas;
	
	public Databases dbs;
	public static MicroWorkloadGenerator mwl;
	int dcId;
	int proxyId;
	
	
	public MicroDBProxy(String dbConfFile, int dcId, double bR, int r, int w, int objC, int keySel) {
		dbs = new Databases();
		dbs.parseXMLfile(dbConfFile);
		dbs.printOut();
		mwl = new MicroWorkloadGenerator(dbs.dbList, dcId, bR, r, w , 0, objC - 1, keySel);
		
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
					.println("usage: MicroDBProxy dc_user_config.xml dcId proxyId db_config.xml threadcount tcpdelay readReqNum writeRedNum bluerate objectCount keySelection connNum");
			System.exit(-1);
		}
		
		MicroDBProxy mbp = new MicroDBProxy(arg[3],Integer.parseInt(arg[1]), Double.parseDouble(arg[8]), 
				Integer.parseInt(arg[6]), Integer.parseInt(arg[7]), Integer.parseInt(arg[9]),Integer.parseInt(arg[10]));
		
		// set up an appServer

		mas = new MicroDBAppServer(arg[0], Integer
				.parseInt(arg[1]), Integer.parseInt(arg[2]), mbp, TestName.MICRODBPROXY, Integer.parseInt(arg[11]));

		// set up the networking for outgoing messages
		NettyTCPSender sendNet1 = new NettyTCPSender();
		mas.setSender(sendNet1);
		boolean tcpnodelay = Boolean.parseBoolean(arg[5]);
		sendNet1.setTCPNoDelay(false);
		sendNet1.setKeepAlive(true);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the appServer
		int threadcount = Integer.parseInt(arg[4]);
                ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(mas, threadcount);
		// then create the actual network
		
		NettyTCPReceiver rcv1 = new NettyTCPReceiver(mas.getMembership()
				.getMe().getInetSocketAddress(), ptnq1, threadcount);
	}
}
