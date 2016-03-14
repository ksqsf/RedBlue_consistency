package applications.microbenchmark.membership;
import util.Debug;

import java.net.InetAddress;

public class User extends Principal {

	private int userId;
	private int dcId;

	public User(int userId, int dcId, String host, int port) {
		super(host, port);
		this.userId = userId;
		this.dcId = dcId;
	}

	public User(int userId, int dcId, InetAddress host, int port) {
		super(host, port);
		this.userId = userId;
		this.dcId = dcId;
	}

	public int getUserId() {
		return userId;
	}

	public int getDatacenterId() {
		return dcId;
	}

	public String toString() {
		return "++ User " + super.toString();
	}

}
