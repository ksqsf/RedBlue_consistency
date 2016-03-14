package applications.microbenchmark.membership;
import util.Debug;

import java.net.InetAddress;

public class AppServer extends Principal {

	private int appServerId;
	private int dcId;

	public AppServer(int appServerId, int dcId, String host, int port) {
		super(host, port);
		this.appServerId = appServerId;
		this.dcId = dcId;
	}

	public AppServer(int appServerId, int dcId, InetAddress host, int port) {
		super(host, port);
		this.appServerId = appServerId;
		this.dcId = dcId;
	}

	public int getAppServerId() {
		return appServerId;
	}

	public int getDatacenterId() {
		return dcId;
	}

	public String toString() {
		return "++ AppServer " + super.toString();
	}

}
