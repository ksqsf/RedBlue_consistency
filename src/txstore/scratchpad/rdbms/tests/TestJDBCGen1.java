package txstore.scratchpad.rdbms.tests;
import util.Debug;

import java.sql.*;
import java.util.*;
import txstore.scratchpad.*;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.jdbc.PassThroughProxy;
import txstore.scratchpad.rdbms.jdbc.TxMudConnection;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.resolution.LWWLockExecution;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBUpdateResult;
import txstore.util.LogicalClock;
import txstore.util.ProxyTxnId;
import txstore.util.ReadWriteSet;
import txstore.util.Result;
import txstore.util.TimeStamp;

public class TestJDBCGen1
{

	public static void main( String[] args) {
		try {
			Debug.debug = true;
			// setup proxy
			ScratchpadConfig config = new ScratchpadConfig( "org.h2.Driver", "jdbc:h2:test", "sa", "", "txstore.scratchpad.rdbms.DBScratchpad");
			config.putPolicy("T1", new LWWLockExecution(false));
			config.putPolicy("T2", new AllOpsLockExecution(false));
			config.putPolicy("T3", new AllOpsLockExecution(true));
			config.putPolicy("T4", new LWWLockExecution(false));
			PassThroughProxy.config = config;
			
			long n = (new java.util.Date().getTime() / 1000) % 100000;


			Class.forName("txstore.scratchpad.rdbms.jdbc.TxMudDriver");
			TxMudConnection con = (TxMudConnection)DriverManager.getConnection( "jdbc:txmud:test");
			
			Statement stat = con.createStatement();
			
			ResultSet rq = stat.executeQuery( "select count(*) from t1;");
			Debug.println( "query result = \n" + rq);
			con.rollback();
			
			con.executeUpdate( DBTestCountOperation.createOperation((int)n,1000,1000,"C"+1000));
			
			con.commit();


			rq = stat.executeQuery( "select * from t1;");
			Debug.println( "query result = \n" + rq);
			con.rollback();

			Debug.println( "Test 1 completed with success");
			
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
}
