package applications.microbenchmark.util;

import util.Debug;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import txstore.proxy.ClosedLoopProxy;
import txstore.proxy.Proxy;
import txstore.scratchpad.ScratchpadConfig;
import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.jdbc.PassThroughProxy;
import txstore.scratchpad.rdbms.jdbc.TxMudConnection;
import txstore.scratchpad.rdbms.jdbc.TxMudDriver;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.resolution.LWWLockExecution;
import txstore.scratchpad.rdbms.util.micro.DBMICROTX1;
import txstore.scratchpad.rdbms.util.micro.DBMICROTX2;
import txstore.storageshim.StorageShim;
import txstore.util.ProxyTxnId;

import applications.microbenchmark.DBTest.MicroDBProxy;
import applications.microbenchmark.ScratchpadTest.ScratchPadProxy;
import applications.microbenchmark.TxMudTest.MicroProxy;
import applications.microbenchmark.messages.MessageBase;
import applications.microbenchmark.messages.MessageFactory;
import applications.microbenchmark.messages.MessageTags;
import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.MicroBaseNode;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;

import applications.microbenchmark.util.ResultPrint;

//for logging
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MicroAppServer extends MicroBaseNode {

	int dcId;
	int appServerId;
	MessageFactory mf;
	MicroProxy mp;
	ClosedLoopProxy imp;
	MicroDBProxy mbp;
	ScratchPadProxy sp;
	MicroWorkloadGenerator mwl;
	static int max_connection;
	// define a hash map here to maintain a connection pool
	Hashtable<ProxyTxnId, TxMudConnection> usedConns;
	Vector<TxMudConnection> unusedConns;
	TestName selectProxy;
	private int connNum;

	/*
	 * long baseTime = 0;
	 * 
	 * private static Logger microAppServerLogger = Logger
	 * .getLogger(MicroAppServer.class.getName()); private static FileHandler
	 * fh;
	 */

	public MicroAppServer(String file_name, int s1, int s2, MicroProxy m,
			ClosedLoopProxy imp2, TestName testName, int cM) {
		super(file_name, s1, Role.APPSERVER, s2);
		dcId = s1;
		appServerId = s2;
		mp = m;
		imp = imp2;
		selectProxy = testName;
		mwl = mp.getMicroWorkloadGenerator();
		connNum = cM;
		init();
	}

	public MicroAppServer(String file_name, int s1, int s2, MicroDBProxy m,
			TestName testName, int cM) {
		super(file_name, s1, Role.APPSERVER, s2);
		dcId = s1;
		appServerId = s2;
		mbp = m;
		selectProxy = testName;
		mwl = mbp.getWorkloadGenerator();
		connNum = cM;
		init();
	}

	public MicroAppServer(String file_name, int s1, int s2, ScratchPadProxy m,
			TestName testName, int cM) {
		super(file_name, s1, Role.APPSERVER, s2);
		dcId = s1;
		appServerId = s2;
		sp = m;
		selectProxy = testName;
		mwl = sp.getWorkloadGenerator();
		connNum = cM;
		Database dataDB = sp.dbs.returnDB(dcId, 0);
		String driver = "";
		Vector<String> connInfo;
		if(dataDB.url_prefix.contains("mysql"))
			driver = "com.mysql.jdbc.Driver";
		else{
			if (dataDB.url_prefix.contains("mimer"))
				driver = "com.mimer.jdbc.Driver";
			
		}
		connInfo = dataDB.getConnInfo();
		String url = dataDB.url_prefix + connInfo.get(0) + ':' + connInfo.get(1) + '/'
				+ connInfo.get(4);
		String user = connInfo.get(2);
		String pwd = connInfo.get(3);
		String padClass = "txstore.scratchpad.rdbms.DBScratchpad";
		ScratchpadConfig config = new ScratchpadConfig(driver, url, user,
				pwd, padClass);
		ArrayList<String> redTableList = mwl.getRedTableList();
		ArrayList<String> blueTableList = mwl.getBlueTableList();
		for(int i = 0; i < redTableList.size();i++){
			String tableName = redTableList.get(i);
			config.putPolicy(tableName, new LWWLockExecution(false));
		}
		
		for(int i = 0; i < blueTableList.size();i++){
			String tableName = blueTableList.get(i);
			config.putPolicy(tableName, new AllOpsLockExecution(true));
		}
		PassThroughProxy.config = config;
		try {
			DBScratchpad.prepareDBScratchpad(config);
		} catch (ScratchpadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		init();
	}

	public void init() {
		mf = new MessageFactory();
		max_connection = 1000;
		usedConns = new Hashtable<ProxyTxnId, TxMudConnection>();
		unusedConns = new Vector<TxMudConnection>();

		for (int i = 0; i < connNum; i++) {
			Debug.printf("Open the %d th connection in advance\n", i);
			unusedConns.add(createNewConnection());
		}
	}

	public TxMudConnection createNewConnection() {
		TxMudConnection conn = null;
		switch (selectProxy) {
		case MICROPROXY:
			// Open a database connection via the new jdbc interface
			try {
				Class.forName("txstore.scratchpad.rdbms.jdbc.TxMudDriver");
				TxMudDriver.proxy = imp;
				try {
					conn = (TxMudConnection) DriverManager.getConnection("jdbc:txmud:test");
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case MICRODBPROXY:
			Database db = mbp.dbs.returnDB(dcId, 0);
			//conn = db.connectToDB();
			try {
				conn.setAutoCommit(false);
			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			break;
		case SCRATCHPADPROXY:
			// setup proxy


			try {
				Class.forName("txstore.scratchpad.rdbms.jdbc.TxMudDriver");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				conn = (TxMudConnection) DriverManager.getConnection("jdbc:txmud:test");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			throw new RuntimeException("That proxy is not recognized.");
		}
		return conn;
	}

	public synchronized TxMudConnection getNewConnection(ProxyTxnId txnId) {
		TxMudConnection conn = null;
		if (!unusedConns.isEmpty()) {
			Debug.printf(
					"connection pool is not empty, get one from it, now its size is %d \n",
					unusedConns.size());
			conn = unusedConns.get(0);
			unusedConns.removeElementAt(0);
		} else {
			if (usedConns.size() < max_connection) {
				Debug.printf(
						"connection pool is empty, try to create a new connection, now its size is %d\n",
						unusedConns.size());
				/*
				 * String tmpStr = "txnId|" + txnId.toString() +
				 * "|START CREAT CONNECTION|" + Long.toString(System.nanoTime()
				 * - baseTime); microAppServerLogger.log(Level.INFO, tmpStr);
				 */
				conn = createNewConnection();
				/*
				 * tmpStr = "txnId|" + txnId.toString() +
				 * "|END CREAT CONNECTION|" + Long.toString(System.nanoTime() -
				 * baseTime); microAppServerLogger.log(Level.INFO, tmpStr);
				 */
			} else {
				throw new RuntimeException("Too many connections");
			}
		}
		usedConns.put(txnId, conn);
		return conn;
	}

	public synchronized void releaseConnection(ProxyTxnId txnId, TxMudConnection conn) {
		// put this conn into unused queue
		unusedConns.add(conn);
		usedConns.remove(txnId);
	}

	public void issueTxn(TxnReqMessage msg){
		try{
		// generate commands here
			
		long startTime =System.nanoTime();
		boolean success = false;
		int userId = msg.getTxnId().getProxyId();
		int dcId = msg.getTxnId().getDcId();
		Debug.printf("this transaction is from dcId %d, user %d\n", dcId, userId);
		requestList rqList = mwl.generate_commands(userId*10 + dcId, msg.isRead());
		ArrayList<String> commands = rqList.getCommands();

		TxMudConnection conn = getNewConnection(msg.getTxnId());

		Statement stat=null;
		try {
			stat = conn.createStatement();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			System.exit(-1);
		}
		
		while (!success) {
			for (int index = 0; index < commands.size(); index++) {
				String request = commands.get(index);
				try {
					if (request.toLowerCase().contains("select") == true) {
						ResultSet rq = stat.executeQuery(request);
						// ResultPrint.microResultsetPrint(rq);
						rq.close();
					} else {
						int ru = stat.executeUpdate(request);
						Debug.println("execute update query");
						Debug.printf("query affect %d rows\n", ru);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
			}
			try {

				stat.close();//release this object
				conn.commit(rqList.getShadowOp(), rqList.getColor());
				success = true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				Debug.println("txn failed need to retry \n");
				if(selectProxy == TestName.MICRODBPROXY){
					try {
						conn.rollback();
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		releaseConnection(msg.getTxnId(), conn);
		int indiLatency = (int) ((System.nanoTime() - startTime)*0.000001);
		if(indiLatency > 500){
			System.out.println("long txn " + msg.getTxnId() + " color " + rqList.getColor() + " read " + msg.isRead() + " latency " + indiLatency);
		}
		Debug.println("txn succeeded \n");
		TxnRepMessage msg1 = new TxnRepMessage(msg.getTxnId(), 0, rqList.getColor());
		replyToUser(dcId, userId, msg1);
		}catch(NullPointerException e){
			e.printStackTrace();
		}
	}

	@Override
	public void handle(byte[] b) {
		// TODO Auto-generated method stub
		MessageBase msg = mf.fromBytes(b);
		Debug.println(msg);
		if (msg == null)
			throw new RuntimeException("Should never receive a null message");

		if (msg.getTag() == MessageTags.TXNREQ) {
			process((TxnReqMessage) msg);

		} else {
			throw new RuntimeException("invalid message tag: " + msg.getTag());
		}
	}

	private void process(TxnReqMessage msg){
		// TODO Auto-generated method stub
		issueTxn(msg);
	}

	public void replyToUser(int dcId, int userId, TxnRepMessage msg) {
		sendToUser(msg, dcId, userId);
	}

}
