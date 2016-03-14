package applications.simplestub;
import util.Debug;

import txstore.util.ProxyTxnId;
import txstore.proxy.ClosedLoopProxyInterface;
import txstore.proxy.ClosedLoopProxy;
import txstore.proxy.ApplicationInterface;

import txstore.util.Operation;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

public class StubProxy implements ApplicationInterface {

	public StubProxy() {
	}

	public int selectStorageServer(Operation op) {
		return 0;
	}

	public int selectStorageServer(byte[] op) {
		return 0;
	}

	public static void main(String arg[]) {
		if (arg.length != 6) {
			System.out
					.println("usage: StubClosedLoopProxy config.xml dcId proxyId opCount objectCount BlueRate");
			System.exit(0);
		}
		ClosedLoopProxy imp = new ClosedLoopProxy(arg[0], Integer.parseInt(arg[1]), Integer
				.parseInt(arg[2]), new StubProxy());

                double blueRate = Double.parseDouble(arg[5]);
		int objectCount = Integer.parseInt(arg[4]);

		// set up the networking for outgoing messages
		NettyTCPSender sendNet = new NettyTCPSender();
		imp.setSender(sendNet);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the proxy
		ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, 2);
		// then create the actual network
		NettyTCPReceiver rcv = new NettyTCPReceiver(
							    imp.getMembership().getMe().getInetSocketAddress(), ptnq);

		// now we're good to go with the application
		byte[] tmp = new byte[45];
		for (int i = 0; i < Integer.parseInt(arg[3]); i++) {

			boolean success = false;
			OperationList opList = new OperationList();
                        System.out.println("need to adjust blue color changes");
			for (int k = 0; k < 4; k++) {
				int id = (i * k + i + i / (k + 1)) % objectCount;
				if (id < 0)
					id = -id;
                                int color = Math.random()<blueRate?1:0;
				opList.addWrite(new Write(id, color));
				id = (i * i + k * k + i / (k + 1)) % objectCount;
				if (id < 0)
					id = -id;
                                color = Math.random()<blueRate?1:0;
				opList.addRead(new Read(id, color));
			}
			System.out.print("\t\t\tReads: ");
			for (int k = 0; k < 4; k++) {
				System.out.print(opList.getReads().elementAt(k) + " ");
			}
			System.out.print("\n\t\t\tWrites: ");
			for (int k = 0; k < 4; k++)
				System.out.print(opList.getWrites().elementAt(k) + " ");
			Debug.println();
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
			Debug.println("*****finished transaction " + i);

		}
		System.exit(0);

	}

}