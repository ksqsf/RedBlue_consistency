package applications.microbenchmark.TxMudTest;

import util.Debug;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;


import applications.microbenchmark.util.Database;
import applications.microbenchmark.util.Databases;
import applications.microbenchmark.util.MicroWorkloadGenerator;
import applications.microbenchmark.util.requestList;


public class TestProxy {

	MicroWorkloadGenerator mwl;
	static int max_connection = 1000;
	// define a hash map here to maintain a connection pool
	Vector<Connection> connPool;
	private int connNum;
	public Databases dbs;
	Random randomGenerator;

	/*
	 * long baseTime = 0;
	 * 
	 * private static Logger microAppServerLogger = Logger
	 * .getLogger(MicroAppServer.class.getName()); private static FileHandler
	 * fh;
	 */
	
	public TestProxy(String dbConfFile, double bR, int r, int w, int objC, int keySel, int cM) {
		dbs = new Databases();
		dbs.parseXMLfile(dbConfFile);
		dbs.printOut();
		mwl = new MicroWorkloadGenerator(dbs.dbList, 0, bR, r, w , 0, objC - 1, keySel);
		connNum = cM;
		connPool = new Vector<Connection>();
		randomGenerator = new Random();
	}

	public void init() {
		max_connection = 1000;

		for (int i = 0; i < connNum; i++) {
			Debug.printf("Open the %d th connection in advance\n", i);
			connPool.add(createNewConnection());
		}
	}

	public Connection createNewConnection() {
		Connection conn = null;
		Database db = dbs.returnDB(0, 0);
		conn = db.connectToDB();
		try {
			conn.setAutoCommit(false);
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		return conn;
	}

	public synchronized Connection getNewConnection() {
		Connection conn = null;
		if (!connPool.isEmpty()) {
			Debug.printf(
					"connection pool is not empty, get one from it, now its size is %d \n",
					connPool.size());
			conn = connPool.get(0);
			connPool.removeElementAt(0);
		} else {
			if (connPool.size() < max_connection) {
				Debug.printf(
						"connection pool is empty, try to create a new connection, now its size is %d\n",
						connPool.size());
				conn = createNewConnection();
				connPool.add(conn);
			} else {
				throw new RuntimeException("Too many connections");
			}
		}
		return conn;
	}

	public synchronized void releaseConnection(Connection conn) {
		// put this conn into unused queue
		connPool.add(conn);
	}

	public void issueTxn(){
		// generate commands here
		boolean success = false;
		int userId = randomGenerator.nextInt(10);
		boolean isRead = true;
		if(randomGenerator.nextInt(2) == 0){
			isRead = false;
		}
		
		requestList rqList = mwl.generate_commands(userId*10, isRead);
		ArrayList<String> commands = rqList.getCommands();

		Connection conn = getNewConnection();

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
				//conn.commit(rqList.getShadowOp(), 0);
				conn.commit();
				success = true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				Debug.println("txn failed need to retry \n");
			}
		}
		releaseConnection(conn);
		Debug.println("txn succeeded \n");
	}
	
	public static void main(String arg[]) {
		if (arg.length != 7) {
			System.out
					.println("usage: TestProxy db_config.xml bluerate readReqNum writeReqNum objectCount keySelection connNum");
			System.exit(-1);
		}
		TestProxy tP = new TestProxy(arg[0], Double.parseDouble(arg[1]), Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), 
				Integer.parseInt(arg[4]), Integer.parseInt(arg[5]), Integer.parseInt(arg[6]));
		long latency = 0;
		long startTime = System.nanoTime();
		for(int i = 0; i < 100000;i++){
			long txnStartTime = System.nanoTime();
			tP.issueTxn();
			latency += System.nanoTime() - txnStartTime;
			
			if(i%2000 == 0){
				System.out.println("txnNum " + i + " throuput " +  (double) (((double) 2000 / (double) (System.nanoTime()-startTime))* (double) 1000000 * (double) 1000) 
						+ " latency " + (double) ((double) latency
								/ (double) 2000 / (double) 1000000));
				startTime = System.nanoTime();
				latency =0;
			}
		}
	}

		
}
