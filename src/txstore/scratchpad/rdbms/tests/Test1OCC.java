package txstore.scratchpad.rdbms.tests;
import util.Debug;

import java.sql.*;
import java.util.*;
import txstore.scratchpad.*;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.OCCDBScratchpad;
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

public class Test1OCC
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
			
			DBUpdateResult ru;
			DBSelectResult rq;
			
			db.beginTransaction( new ProxyTxnId(0,0,0));

			ru = (DBUpdateResult)db.execute( new DBSingleOperation( "insert into t1 (a,b,c,d,e) values (" + n + ", " + (n%10) + ",2,3,\'S" + (n%100) + "\');"));
			System.out.println( "result = " + ru);

			ru = (DBUpdateResult)db.execute( new DBSingleOperation( "insert into t1 (a,b,c,d,e) values (" + (n+1) + ", " + ((n+1)%10) + ",2,3,\'S" + ((n+1)%100) + "\');"));
			System.out.println( "result = " + ru);

			ru = (DBUpdateResult)db.execute( new DBSingleOperation( "insert into t2 (a,b,c,d,e) values (" + n + ", " + (n%5) + ",2,3,\'T" + (n%100) + "\');"));
			System.out.println( "result = " + ru);

			rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t1 where a > 10000 limit 2;"));
			System.out.println( "query result = \n" + rq);
			
			if( rq.next()) {
				ru =  (DBUpdateResult)db.execute( new DBSingleOperation( "delete from t1 where a=" + rq.getInt(1) + ";"));
				System.out.println( "result = " + ru);
				if( rq.next()) {
					ru =  (DBUpdateResult)db.execute( new DBSingleOperation( "update t1 set c = c + 1 where a =" + rq.getInt(1) + ";"));
					System.out.println( "result = " + ru);
				}
			}

			ReadWriteSet rwset = db.complete();
			System.out.println( "complete = " + rwset);
			
			long []dcs = { 1, 2};
			LogicalClock lc = new LogicalClock( dcs, 1);
			TimeStamp ts = new TimeStamp( 1, n);
			
			db.commit( lc, ts);

			db.beginTransaction( new ProxyTxnId(0,0,1));
			rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t1;"));
			System.out.println( "query result = \n" + rq);
			rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t2;"));
			System.out.println( "query result = \n" + rq);
			db.abort();

			

			System.out.println( "Test 1 completed with success");
			
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
}
