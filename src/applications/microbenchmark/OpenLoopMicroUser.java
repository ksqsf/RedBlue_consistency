package applications.microbenchmark;

import util.Debug;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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

import network.ParallelPassThroughNetworkQueue;
import network.PassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;

//for logging
import org.apache.log4j.*;

import org.apache.zookeeper.KeeperException;

//barrier op

import applications.microbenchmark.util.SyncPrimitive;
import applications.microbenchmark.util.SyncPrimitive.Barrier;
import applications.microbenchmark.util.SyncPrimitive.BarrierExp2;
import applications.microbenchmark.util.TxnRecord;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenLoopMicroUser extends MicroBaseNode {

	int dcId;
	int connectDcId;
	int appServerId;
	int userId;
	MessageFactory mf;
	long count;
	long committedCount = 0;
	long intervalCommittedCount = 0;
	long intervalLatency = 0;
	long globalStartTime = 0;
	long startTime = 0;
	static long  baseTime = 0;
	static boolean wantLatency = false;
	int outputInterval = 0;
	Random rand;
	static double writeRatio;
	static boolean primaryBackup = false;
	
	//measurement
	static long warmUpTime = 0;
	static long measurementDuration = 0;
	long startMeasureTime = 0;
	long endMeasureTime = 0;
	long coolDownTime = Long.parseLong("60000000000");
	long measureTxnNum = 0;
	long measureLatency = 0;
	boolean startMeasured = false;

	Hashtable<ProxyTxnId, TxnRecord> records = new Hashtable<ProxyTxnId, TxnRecord>();

	//latency distribution 
	Hashtable<Integer, AtomicInteger> redTxnLatenD = new Hashtable<Integer, AtomicInteger>();
	Hashtable<Integer, AtomicInteger> blueTxnLatenD = new Hashtable<Integer, AtomicInteger>();
	Hashtable<Integer, AtomicInteger> readTxnLatenD = new Hashtable<Integer, AtomicInteger>();
	Hashtable<Integer, AtomicInteger> writeTxnLatenD = new Hashtable<Integer, AtomicInteger>();
	long readTxnNum = 0;
	long writeTxnNum = 0;
	long readTxnLaten = 0;
	long writeTxnLaten = 0;
	long redTxnNum = 0;
	long blueTxnNum = 0;
	long redTxnLaten = 0;
	long blueTxnLaten = 0;
	
	static Logger logger = Logger.getLogger(OpenLoopMicroUser.class.getName());

	public OpenLoopMicroUser(String file_name, int s1, int s2, int s3, int oI, int c1) {

		super(file_name, s1, Role.USER, s3);

		dcId = s1;
		connectDcId = c1;
		appServerId = s2;
		userId = s3;
		mf = new MessageFactory();
		count = 0;
		outputInterval = oI;
		String logFileName = "user" + Integer.toString(dcId)+"-"+Integer.toString(userId) + "-.log";
		FileAppender fileappender = null;
		try {
			fileappender = new FileAppender(new PatternLayout(), logFileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.addAppender(fileappender);
		rand = new Random(appServerId);
	}

	public synchronized ProxyTxnId nextTxnId() {

		ProxyTxnId txnId = new ProxyTxnId(dcId, userId, count++);
		return txnId;

	}

	public void startTxn() {
		synchronized(this){
			long currentTime = System.nanoTime();
			if(currentTime >= endMeasureTime + coolDownTime){
				statisticOutput();
				System.out.println("measurement time reached\n");
				System.exit(0);
			}
		}
		
		if(intervalCommittedCount == 0){
			startTime = System.nanoTime() - baseTime;
			intervalLatency = 0;
		}
		if(committedCount == 0){
			globalStartTime = System.nanoTime() - baseTime;
		}
		TxnRecord rec = new TxnRecord(nextTxnId());
		records.put(rec.getTxnId(), rec);
		int read = Math.random() < writeRatio ? 1:0;
		TxnReqMessage msg = new TxnReqMessage(rec.getTxnId(),read);
		if(read == 1){
			rec.setUpdate();
		}
		if(read==1 && primaryBackup)//read 1 => write
			sendToAppServer(msg, 0, appServerId);
		else
			sendToAppServer(msg, connectDcId,appServerId);

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

	private void process(TxnRepMessage msg) {
		if(records.get(msg.getTxnId()) == null){
			System.out.println("I don't have this transaction " + msg);
		}
		TxnRecord rec = records.get(msg.getTxnId());
		//rec.finish();
		rec.setEnd(System.nanoTime());
		rec.setColor(msg.getColor());
		statisticMeasure(rec);
		records.remove(msg.getTxnId());
		startTxn();
	}
	
	/**
	 * Measure data in the given period
	 * @param rec
	 */
	public void statisticMeasure(TxnRecord rec){
		//distribution
		if(rec.getStartTime() >= startMeasureTime && rec.getEndTime() <= endMeasureTime ){
			synchronized(this){
				if(startMeasured == false){
					logger.info("measurement started: " + startMeasureTime );
					startMeasured = true;
				}
				measureTxnNum++;
				measureLatency += rec.latency();
				int indiLatency = (int) (rec.latency()*0.000001);
				if(indiLatency > 50){
					System.out.println("long txn " + rec.getTxnId() + " color " + rec.getColor() + " read " + rec.isReadonly() + " latency " + indiLatency);
				}
				if(rec.isReadonly()){
					if(readTxnLatenD.get(indiLatency) == null)
						readTxnLatenD.put(indiLatency, new AtomicInteger(1));
					else
						readTxnLatenD.get(indiLatency).incrementAndGet();
					readTxnNum++;
					readTxnLaten += rec.latency();
				}else{
					if(writeTxnLatenD.get(indiLatency) == null)
						writeTxnLatenD.put(indiLatency, new AtomicInteger(1));
					else
						writeTxnLatenD.get(indiLatency).incrementAndGet();
					writeTxnNum++;
					writeTxnLaten += rec.latency();
				}
				if(rec.getColor() == 0){
					if(redTxnLatenD.get(indiLatency) == null)
						redTxnLatenD.put(indiLatency, new AtomicInteger(1));
					else
						redTxnLatenD.get(indiLatency).incrementAndGet();
					redTxnNum++;
					redTxnLaten += rec.latency();
				}else{
					if(blueTxnLatenD.get(indiLatency) == null)
						blueTxnLatenD.put(indiLatency, new AtomicInteger(1));
					else
						blueTxnLatenD.get(indiLatency).incrementAndGet();
					blueTxnNum++;
					blueTxnLaten += rec.latency();
				}
			}
		}
		committedCount++;
		intervalCommittedCount++;
		intervalLatency += rec.latency();
		if (committedCount % outputInterval == 0){
			long endTime = System.nanoTime() - baseTime;
			String tmpStr = "totalrequest " 
							+ committedCount 
							+ " startTime  (ns)     "
							+ startTime
							+" totaltime (ns) " 
							+ (endTime-startTime) 
							+ " throughput (op/s)   "
							+ (double) ((double) outputInterval / (double) (endTime-startTime)
										* (double) 1000000 * (double) 1000)
							+ " totallatency (ns) " 
							+ intervalLatency
							+ " latency    (ms/req) "
							+ (double) ((double) intervalLatency
									/ (double) outputInterval / (double) 1000000)
							+ " total thpt (op/s)    "
							+ (double) ((double) committedCount
									/ (double) (endTime - globalStartTime)
									* (double) 1000000 * (double) 1000);
			logger.info(tmpStr);
			intervalCommittedCount = 0;
		}
	}
	/**
	 * final compute the result and output
	 */
	
	public void statisticOutput(){
		String tmpStr = "startMeasure (ns) " 
						+ startMeasureTime 
						+ " endMeasure  (ns)     "
						+ endMeasureTime
						+" totaltime (ns) " 
						+ measurementDuration
						+ " totaltxnnum " 
						+ measureTxnNum
						+ " totalthroughput (op/s)   "
						+ (double) ((double) measureTxnNum / (double) measurementDuration
									* (double) 1000000 * (double) 1000)
						+ " totallatency (ns) " 
						+ measureLatency
						+ " latency (ms/req) "
						+ (double) ((double) measureLatency
								/ (double) measureTxnNum / (double) 1000000);
		if(redTxnNum >0){
			tmpStr += " redtxnnum " + redTxnNum;
			tmpStr += " redthroughput(op/s) " 
				+ (double) ((double) redTxnNum / (double) measurementDuration
						* (double) 1000000 * (double) 1000)
						+ " totalredlatency(ns) " + redTxnLaten
						+ " redlatency(ms/req) " 
						+ (double) ((double) redTxnLaten
								/ (double) redTxnNum / (double) 1000000);
		}
		if(blueTxnNum > 0){
			tmpStr += " bluetxnnum " + blueTxnNum;
			tmpStr += " bluethroughput(op/s) " 
				+ (double) ((double) blueTxnNum / (double) measurementDuration
						* (double) 1000000 * (double) 1000)
						+ " totalbluelatency(ns) " + blueTxnLaten
						+ " bluelatency(ms/req) " 
						+ (double) ((double) blueTxnLaten
								/ (double) blueTxnNum / (double) 1000000);
		}
		
		if(readTxnNum > 0){
			tmpStr += " readtxnnum " + readTxnNum;
			tmpStr += " readthroughput(op/s) " 
				+ (double) ((double) readTxnNum / (double) measurementDuration
						* (double) 1000000 * (double) 1000)
						+ " totalreadlatency(ns) " + readTxnLaten
						+ " readlatency(ms/req) " 
						+ (double) ((double) readTxnLaten
								/ (double) readTxnNum / (double) 1000000);
		}
		
		if(writeTxnNum > 0){
			tmpStr += " writetxnnum " + writeTxnNum;
			tmpStr += " writethroughput(op/s) " 
				+ (double) ((double) writeTxnNum / (double) measurementDuration
						* (double) 1000000 * (double) 1000)
						+ " totalwritelatency(ns) " + writeTxnLaten
						+ " writelatency(ms/req) " 
						+ (double) ((double) writeTxnLaten
								/ (double) writeTxnNum / (double) 1000000);
		}
		logger.info(tmpStr);		
		
		Integer[] redkeys = (Integer[]) redTxnLatenD.keySet().toArray(new Integer[0]);
		Arrays.sort(redkeys);
		for(Integer k : redkeys){
			int num = redTxnLatenD.get(k).get();
			tmpStr = "RLD " + k.intValue() + " " + num;
			logger.info(tmpStr);
		}
		
		Integer[] bluekeys = (Integer[]) blueTxnLatenD.keySet().toArray(new Integer[0]);
		Arrays.sort(bluekeys);
		for(Integer k : bluekeys){
			int num = blueTxnLatenD.get(k).get();
			tmpStr = "BLD " + k.intValue() + " " + num;
			logger.info(tmpStr);
		}
		
		Integer[] readkeys = (Integer[]) readTxnLatenD.keySet().toArray(new Integer[0]);
		Arrays.sort(readkeys);
		for(Integer k : readkeys){
			int num = readTxnLatenD.get(k).get();
			tmpStr = "ReadLD " + k.intValue() + " " + num;
			logger.info(tmpStr);
		}
		
		Integer[] writekeys = (Integer[]) writeTxnLatenD.keySet().toArray(new Integer[0]);
		Arrays.sort(writekeys);
		for(Integer k : writekeys){
			int num = writeTxnLatenD.get(k).get();
			tmpStr = "WLD " + k.intValue() + " " + num;
			logger.info(tmpStr);
		}
		
	}

	public static void main(String arg[]) {
		if (arg.length != 13) {
			System.out
					.println("usage: MicroUser dc_user_config.xml dcId proxyId userId outstandingOps outputInterval connectDcId wantLatency readwriteRatio primarybackup thdcount startMeasureTime measurementDuration");
			System.exit(-1);
		}

		OpenLoopMicroUser mcUser = new OpenLoopMicroUser(arg[0],
				Integer.parseInt(arg[1]), Integer.parseInt(arg[2]),
				Integer.parseInt(arg[3]), Integer.parseInt(arg[5]), Integer.parseInt(arg[6]));

		wantLatency = Boolean.parseBoolean(arg[7]);
		writeRatio = Double.parseDouble(arg[8]);
		primaryBackup = Boolean.parseBoolean(arg[9]);
		int userThdCount = Integer.parseInt(arg[10]);
		//set measurement parameters
		warmUpTime = Long.parseLong(arg[11]);
		measurementDuration = Long.parseLong(arg[12]);
		// set up the networking for outgoing messages
		NettyTCPSender sendNet = new NettyTCPSender();
		mcUser.setSender(sendNet);
		sendNet.setTCPNoDelay(false);
		sendNet.setKeepAlive(true);

		// set up the networking for incoming messages
		// first, create the pipe from the network to the user
		ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(mcUser, 5);
		//PassThroughNetworkQueue ptnq = new PassThroughNetworkQueue(mcUser);
		// then create the actual network
		NettyTCPReceiver rcv = new NettyTCPReceiver(mcUser.getMembership()
			.getMe().getInetSocketAddress(), ptnq,5);
		//NettyTCPReceiver rcv = new NettyTCPReceiver(mcUser.getMembership()
			//.getMe().getInetSocketAddress(), ptnq);

		Principal z = mcUser.getMembership().getPrincipal(
				Integer.parseInt(arg[1]), Role.ZOOKEEPER, 0);
		mcUser.getMembership().getPrincipalCount();
		String[] tmp = z.getHost().toString().split("/");
		String address = tmp[1] + ":10000";
		int userNum = mcUser.getMembership().getAllUserCount();
		System.out.printf("zookeeper host is %s and userNum is %d\n", address,
				userNum);
		// create a barrier object and wait to leave from this barrier

		try {
			Thread.sleep(10);// allow script to check whether user is already
								// set up or not
		} catch (InterruptedException e) {

		}

		BarrierExp2 b = new BarrierExp2(address, "/TxMudExp", arg[1]+arg[3], userNum);
		try {
			b.enter();
			Debug.println("Entered barrier: " + Integer.toString(userNum));
		} catch (KeeperException ex) {
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		Debug.println("Left barrier");
		
		baseTime = System.nanoTime();
		String tmpStr = "userStartedTime (ns) " + baseTime;
		logger.info(tmpStr);
		
		//set measurement parameters:
		mcUser.startMeasureTime = baseTime + warmUpTime;
		mcUser.endMeasureTime = baseTime + warmUpTime + measurementDuration;
		logger.info("warmUpTime (ns): " + warmUpTime + 
				" startMeasure (ns): " + mcUser.startMeasureTime +
				" endMeasure (ns): " + mcUser.endMeasureTime);
		//start first a few transactions here
		for (int i = 0; i < Long.parseLong(arg[4]); i++) {
			mcUser.startTxn();
			/*try {
				Thread.currentThread().wait(10);
			} catch (Exception e) {
			}*/
		}
		
	}

}


