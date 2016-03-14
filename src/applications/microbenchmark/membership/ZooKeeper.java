package applications.microbenchmark.membership;
import util.Debug;

import java.net.InetAddress;

public class ZooKeeper extends Principal{


	public ZooKeeper(String host, int port) {
		super(host, port);
	}

	public String toString() {
		return "++ ZooKeeper " + super.toString();
	}

}
