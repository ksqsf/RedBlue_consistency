package applications.microbenchmark.FakedScratchpadTest;

import util.Debug;


import txstore.membership.Membership;
import txstore.membership.Role;
import txstore.storageshim.StorageShim;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

public class FakedStorage {
	
    public static void main(String arg[]){

	if (arg.length != 8){
	    System.out.println("usage: StubStorage config.xml dcId stroageId userNum threadcount tcpnodelay scratchpadNum objectCount");
	    System.exit(0);
	}

	Membership mem = new Membership(arg[0], Integer.parseInt(arg[1]), Role.STORAGE, Integer.parseInt(arg[2]));
	StorageShim imp = new StorageShim(arg[0], Integer.parseInt(arg[1]), 
					  Integer.parseInt(arg[2]), new FakedScratchpadFactory(mem.getDatacenterCount(),Integer.parseInt(arg[3]), Integer.parseInt(arg[6]),Integer.parseInt(arg[7])));

	// set up the networking for outgoing messages
	NettyTCPSender sendNet = new NettyTCPSender();
	imp.setSender(sendNet);
	sendNet.setTCPNoDelay(Boolean.parseBoolean(arg[5]));
	
	// set up the networking for incoming messages
	// first, create the pipe from the network to the storage
	int threadcount = Integer.parseInt(arg[4]);
        ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, threadcount);
	// then create the actual network
	
	NettyTCPReceiver rcv =  new NettyTCPReceiver(imp.getMembership().getMe().getInetSocketAddress(), ptnq, 2);


    }

}
