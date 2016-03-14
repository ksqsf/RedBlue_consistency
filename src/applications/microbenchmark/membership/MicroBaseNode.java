package applications.microbenchmark.membership;
import util.Debug;

import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.Membership;
import applications.microbenchmark.membership.AppServer;
import applications.microbenchmark.membership.Principal;
import applications.microbenchmark.membership.Datacenter;
import applications.microbenchmark.membership.User;

import util.UnsignedTypes;
import txstore.messages.Message;

import network.ByteHandler;
import network.NetworkSender;

import java.security.interfaces.*;
import javax.crypto.*;

import java.util.Calendar;

/**
 * This class is a baseline for implementing the user/appServer. It implements
 * the ByteHandler interface (handle(byte[])) and provides a collection of
 * wrapper functions for sending to specific components of the system.
 **/

abstract public class MicroBaseNode extends Throwable implements ByteHandler {

	protected Membership members;

	protected NetworkSender sendNet = null;

	protected int myDC, myId;
	protected Role myRole;

	public MicroBaseNode(String membershipFile, int datacenter, Role myRole,
			int myId) {
		members = new Membership(membershipFile, datacenter, myRole, myId);
		myDC = datacenter;
		this.myRole = myRole;
		this.myId = myId;
	}

	public int getMyDatacenterId() {
		return myDC;
	}

	public Role getMyRole() {
		return myRole;
	}

	public int getMyId() {
		return myId;
	}

	public Membership getMembership() {
		return members;
	}

	public int getDatacenterCount() {
		return members.getDatacenterCount();
	}

	public int getAppServerCount() {
		return members.getAppServerCount();
	}
	
	public int getUserCount(){
		return members.getUserCount();
	}

	public void sendToAppServer(Message msg, int dcId, int appServerId){
		send(msg, getMembership().getDatacenter(dcId).getAppServer(appServerId));
	}
	
	public void sendToUser(Message msg, int dcId, int uId){
		send(msg, getMembership().getDatacenter(dcId).getUser(uId));
	}

	private void send(Message msg, Principal rcpt) {
		if (sendNet != null)
			sendNet.send(msg.getBytes(), rcpt.getInetSocketAddress());
		else
			throw new RuntimeException("no sending network available! "
					+ sendNet);
	}

	/**
	 * Listen to appropriate sockets and call handle on all appropriate incoming
	 * messages
	 **/
	public void start() {
		if (sendNet == null) {
			throw new RuntimeException("dont have a network");
		} else {
			// Debug.println("wtf");
		}
	}

	public void setSender(NetworkSender sender) {
		Debug.println("set up the sender network");
		sendNet = sender;
	}

	public void stop() {

		// throw new RuntimeException("Not yet implemented");
	}

	public void printTime(boolean isStart) {
		StackTraceElement[] elements = getStackTrace();
		Calendar now = Calendar.getInstance();
		String startEnd = isStart ? "Starting " : "Ending   ";
		String toPrint = now.getTimeInMillis() + ": " + startEnd
				+ elements[0].getClassName() + "."
				+ elements[0].getMethodName();
		System.err.println(toPrint);
	}

}
