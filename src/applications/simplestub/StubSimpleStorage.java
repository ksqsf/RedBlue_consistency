package applications.simplestub;
import util.Debug;


import txstore.membership.Membership;
import txstore.membership.Role;
import txstore.storageshim.SimpleStorage;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

public class StubSimpleStorage{



    public static void main(String arg[]){

	if (arg.length != 7){
	    System.out.println("usage: StubStorage config.xml dcId stroageId  objectCount threads tokensize tcpnodelay");
	    System.exit(0);
	}

        System.out.println("config: "+arg[0]);
        System.out.println("dc    : "+arg[1]);
        System.out.println("storag: "+arg[2]);
        System.out.println("objs  : "+arg[3]);
        System.out.println("thrds : "+arg[4]);
        System.out.println("toknsz: "+arg[5]);
        System.out.println("tcpno : "+arg[6]);
	Membership mem = new Membership(arg[0], Integer.parseInt(arg[1]), Role.STORAGE, Integer.parseInt(arg[2]));
	SimpleStorage imp = new SimpleStorage(arg[0], Integer.parseInt(arg[1]), 
					  Integer.parseInt(arg[2]), 
                                          new ScratchpadFactory(mem.getDatacenterCount(),
                                                                Integer.parseInt(arg[3]),
                                                                Integer.parseInt(arg[4])),
                                            Integer.parseInt(arg[5]));;

	// set up the networking for outgoing messages
	NettyTCPSender sendNet = new NettyTCPSender();
	imp.setSender(sendNet);
	sendNet.setTCPNoDelay(Boolean.parseBoolean(arg[6]));
        
	// set up the networking for incoming messages
	// first, create the pipe from the network to the proxy
        int threadcount = Integer.parseInt(arg[4]);
	ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, threadcount);
	// then create the actual network
        
	NettyTCPReceiver rcv =  new NettyTCPReceiver(imp.getMembership().getMe().getInetSocketAddress(), ptnq, 4);


    }
}