package txstore.scratchpad.rdbms.tests;
import util.Debug;

import java.sql.*;
import java.util.*;
import txstore.scratchpad.*;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.OCCDBScratchpad;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.resolution.AllOpsOCCExecution;
import txstore.scratchpad.rdbms.resolution.LWWLockExecution;
import txstore.scratchpad.rdbms.resolution.LWWOCCExecution;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBSingleOperation;
import txstore.scratchpad.rdbms.util.DBUpdateResult;
import txstore.util.LogicalClock;
import txstore.util.ProxyTxnId;
import txstore.util.ReadWriteSet;
import txstore.util.Result;
import txstore.util.TimeStamp;

public class Test2OCC
{

	public static void main( String[] args) {
		try {
			Class.forName("com.mimer.jdbc.Driver");
			ScratchpadConfig config = new ScratchpadConfig( "com.mimer.jdbc.Driver", "jdbc:mimer://localhost/testdb2", "MIMER_STORE", "GoodiesRUs", "txstore.scratchpad.rdbms.OCCDBScratchpad");
			config.putPolicy("T1", new LWWOCCExecution(false));
			config.putPolicy("T2", new AllOpsOCCExecution(false));
			config.putPolicy("T3", new AllOpsOCCExecution(true));
			config.putPolicy("T4", new LWWOCCExecution(false));
			
			long n = (new java.util.Date().getTime() / 1000) % 100000;
			
			DBScratchpad db = new OCCDBScratchpad( config);
			db.beginTransaction( new ProxyTxnId(0,0,0));

			
			DBScratchpad db2 = new OCCDBScratchpad( config);
			db.beginTransaction( new ProxyTxnId(0,1,0));
			
			DBUpdateResult ru;
			DBSelectResult rq;
			
			ru = (DBUpdateResult)db.execute( new DBSingleOperation( "insert into t1 (a,b,c,d,e) values (" + n + ", " + (n%10) + ",2,3,\'S" + (n%100) + "\');"));
			System.out.println( "result = " + ru);

			ru = (DBUpdateResult)db2.execute( new DBSingleOperation( "insert into t1 (a,b,c,d,e) values (" + n + ", " + (n%20) + ",3,4,\'P" + (n%200) + "\');"));
			System.out.println( "result = " + ru);

			ReadWriteSet rwset = db.complete();
			System.out.println( "complete = " + rwset);
			
			ReadWriteSet rwset2 = db2.complete();
			System.out.println( "complete = " + rwset);
			
			long []dcs = { 1, 2};
			LogicalClock lc = new LogicalClock( dcs, 1);
			TimeStamp ts = new TimeStamp( 1, n);
			
			db.commit( lc, ts);

			LogicalClock lc2 = new LogicalClock( dcs, 1);
			TimeStamp ts2 = new TimeStamp( 1, n+1);

			db2.commit( lc2, ts2);

			db.beginTransaction( new ProxyTxnId(0,1,0));
			rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t1;"));
			System.out.println( "query result = \n" + rq);
			rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t2;"));
			System.out.println( "query result = \n" + rq);
			db.abort();

			

			Debug.println( "Test 2 completed with success");
			
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
}
