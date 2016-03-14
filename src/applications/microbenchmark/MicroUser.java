package applications.microbenchmark;
import util.Debug;

import java.io.IOException;
import java.util.Random;
import java.lang.InterruptedException;

import txstore.util.ProxyTxnId;

import applications.microbenchmark.messages.MessageBase;
import applications.microbenchmark.messages.MessageFactory;
import applications.microbenchmark.membership.Principal;
import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.MicroBaseNode;
import applications.microbenchmark.membership.ZooKeeper;
import applications.microbenchmark.messages.MessageTags;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;

import network.PassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;

//for logging
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.zookeeper.KeeperException;

//barrier op

import applications.microbenchmark.util.SyncPrimitive;
import applications.microbenchmark.util.SyncPrimitive.Barrier;
import applications.microbenchmark.util.SyncPrimitive.BarrierExp2;

public class MicroUser extends MicroBaseNode {

	int dcId;
	int appServerId;
	int userId;
	static float ratio;
	static int txnNum;
	MessageFactory mf;
	long count;
	boolean isReplied;
	int keySelection;
	int workloadType;
	
	private static Logger userLogger = Logger.getLogger(MicroUser.class.getName());
	private static FileHandler fh;

	public MicroUser(String file_name, int s1, int s2, int s3, float r, int t, int k, int w) {
		super(file_name, s1, Role.USER, s3);
		dcId = s1;
		appServerId = s2;
		userId = s3;
		ratio = r;
		txnNum = t;
		mf = new MessageFactory();
		count = 0;
		isReplied = false;
		keySelection = k;
		workloadType = w;
		Debug.printf("key selection is %d\n", k);
		String logFileName = "user" + Integer.toString(userId) + "-.log" ;
		try {
			fh = new FileHandler(logFileName, true);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		userLogger.setLevel(Level.INFO);
	   SimpleFormatter formatter = new SimpleFormatter();
	   fh.setFormatter(formatter);
	   userLogger.addHandler(fh);
	}

	public ProxyTxnId nextTxnId() {

		ProxyTxnId txnId = new ProxyTxnId(dcId, userId, count++);
		return txnId;

	}

	public void startTxn(int color) {
		TxnReqMessage msg = new TxnReqMessage(nextTxnId(), color, keySelection, workloadType);
		setReply();
		sendToAppServer(msg, appServerId);
		waitForReply();
	}

	@Override
	public void handle(byte[] b) {
		// TODO Auto-generated method stub
		MessageBase msg = mf.fromBytes(b);
		Debug.println(msg);
		if (msg == null)
			throw new RuntimeException("Should never receive a null message");

		if (msg.getTag() == MessageTags.TXNREP) {
			process((TxnRepMessage) msg);

		} else {
			throw new RuntimeException("invalid message tag: " + msg.getTag());
		}
	}
	
	private synchronized void process(TxnRepMessage msg) {
		// TODO Auto-generated method stub
		if(isReplied){
			throw new RuntimeException("isReplied is already set!");
		}
		isReplied = true;
		notifyAll();
	}

	public synchronized void waitForReply() {
		while (isReplied == false) {
			try {
				Debug.println("\t\tMicroUser.waitForReply() 5000 wait");
				wait(5000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public synchronized void setReply(){
		isReplied = false;
	}
	
	public static void main(String arg[]) {
		if (arg.length != 8) {
			System.out
					.println("usage: MicroUser dc_user_config.xml dcId proxyId userId ratio txnNum keyselection workloadType");
			System.exit(-1);
		}

		MicroUser mcUser = new MicroUser(arg[0], Integer.parseInt(arg[1]),
				Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), Float
						.parseFloat(arg[4]), Integer.parseInt(arg[5]), Integer.parseInt(arg[6]),Integer.parseInt(arg[7]));

		// set up the networking for outgoing messages
		NettyTCPSender sendNet = new NettyTCPSender();
		mcUser.setSender(sendNet);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the user
		PassThroughNetworkQueue ptnq = new PassThroughNetworkQueue(mcUser);
		// then create the actual network
		NettyTCPReceiver rcv = new NettyTCPReceiver(mcUser.getMembership()
				.getMe().getInetSocketAddress(), ptnq);

		Principal z = mcUser.getMembership().getPrincipal(Integer.parseInt(arg[1]),
				Role.ZOOKEEPER, 0);
		mcUser.getMembership().getPrincipalCount();
		String[] tmp = z.getHost().toString().split("/");
		String address = tmp[1]+":10000";
		int userNum = mcUser.getMembership().getAllUserCount();
		Debug.printf("zookeeper host is %s and userNum is %d\n", address, userNum);
		//create a barrier object and wait to leave from this barrier
		
		 try {
             Thread.sleep(10);//allow script to check whether user is already set up or not
         } catch (InterruptedException e) {

         } 
		
		BarrierExp2 b = new BarrierExp2(address, "/TxMudExp", arg[3], userNum);
        try {
            b.enter();
            Debug.println("Entered barrier: " + Integer.toString(userNum));
        } catch (KeeperException ex) {
        		ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Debug.println("Left barrier");
		
		//generate requres and send them to appServer
		Random rand = new Random(Integer.parseInt(arg[3]));
		long baseTime = System.nanoTime();
		userLogger.log(Level.INFO, "start time is: "+Long.toString(baseTime));
		Debug.println(baseTime);
		
		long totalTxn = 0;
		long intervalTxn = 0;
		long intervalLatency = 0;
		long intervalStartTime = 0;
		long globalStartTime = 0;
		for(int i = 0; i < txnNum; i++){
			int color = 0;
			if(rand.nextFloat() <= ratio){
				color = 1;//blue
			}
			long startTime = System.nanoTime() - baseTime;
			mcUser.startTxn(color);
			long endTime = System.nanoTime() - baseTime;
			totalTxn++;
			intervalTxn++;
			intervalLatency += endTime - startTime;
			if(intervalTxn == 1){
				intervalStartTime = startTime;
			}
			if(i == 0){
				globalStartTime = System.nanoTime() -baseTime;
			}
			if(intervalTxn%1000 == 0){
				String tmpStr = "totalrequest " 
						+ totalTxn 
						+ " startTime  (ns)     "
						+ intervalStartTime
						+" totaltime (ns) " 
						+ (endTime-intervalStartTime) 
						+ " throughput (op/s)   "
						+ (double) ((double) 1000 / (double) (endTime-intervalStartTime)
									* (double) 1000000 * (double) 1000)
						+ " totallatency (ns) " 
						+ intervalLatency
						+ " latency    (ms/req) "
						+ (double) ((double) intervalLatency
								/ (double) 1000 / (double) 1000000)
						+ " total thpt (op/s)    "
						+ (double) ((double) totalTxn
								/ (double) (endTime - globalStartTime)
								* (double) 1000000 * (double) 1000);
				intervalTxn = 0;
				intervalLatency = 0;
				
				userLogger.log(Level.INFO, tmpStr);
			}
		}
		fh.close();
		Debug.println("I am finishing all transactions requests\n");
		System.exit(0);
	}

}
