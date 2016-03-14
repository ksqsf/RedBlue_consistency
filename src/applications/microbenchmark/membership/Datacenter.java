package applications.microbenchmark.membership;
import util.Debug;

public class Datacenter {
	
	AppServer appServers[];
	User users[];
	ZooKeeper z;
	int dcId;

	public Datacenter(int dcId, AppServer as[], User u[], ZooKeeper z) {
		appServers = as;
		users = u;
		this.z = z; 
		this.dcId = dcId;
	}

	public int getDatacenterId() {
		return dcId;
	}

	public AppServer[] getAppServers() {
		return appServers;
	}

	public AppServer getAppServer(int i) {
		return appServers[i];
	}
	
	public int getAppServersCount() {
		return appServers.length;
	}

	public User[] getUsers() {
		return users;
	}

	public User getUser(int i) {
		String tmp="";
	/*for (int j = 0; j < getUsers().length; j++)
		tmp += "\n\t" + getUsers()[j];
	Debug.printf("getUser users are: %s\n",tmp);*/
		return users[i];
	}

	public int getUsersCount() {
		return users.length;
	}

	public Principal getPrincipal(Role role, int roleid) {
		switch (role) {
		case APPSERVER:
			return getAppServer(roleid);
		case USER:
			return getUser(roleid);
		case ZOOKEEPER:
			return getZooKeeper();
		default:
			throw new RuntimeException("Unkown role: " + role);
		}
	}

	private Principal getZooKeeper() {
		// TODO Auto-generated method stub
		return z;
	}

	public String toString() {
		String tmp = "";
		for (int i = 0; i < getAppServers().length; i++)
			tmp += "\n\t" + getAppServers()[i];
		for (int i = 0; i < getUsers().length; i++)
			tmp += "\n\t" + getUsers()[i];
		return tmp;
	}

}
