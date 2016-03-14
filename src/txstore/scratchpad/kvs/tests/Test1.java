package txstore.scratchpad.kvs.tests;
import util.Debug;

import java.sql.*;
import java.util.*;

import com.sleepycat.bind.tuple.*;

import txstore.scratchpad.*;
import txstore.scratchpad.kvs.*;
import txstore.scratchpad.kvs.resolution.*;
import txstore.scratchpad.kvs.tests.data.*;
import txstore.scratchpad.kvs.util.*;
import txstore.util.*;

public class Test1
{

	public static void main( String[] args) {
		try {
			Debug.debug = true;
			KVScratchpadConfig config = new KVScratchpadConfig( "testbdb", "txstore.scratchpad.kvs.KVScratchpad");

			config.putTableInfo("T1", new KVAbstractExecution("T1", false), "com.sleepycat.bind.tuple.LongBinding", "txstore.scratchpad.kvs.tests.data.MicroTupleBinding");
			TupleBinding t1KB = new LongBinding();
			TupleBinding t1DB = new MicroTupleBinding();
			config.putTableInfo("T2", new KVAbstractExecution("T2", false), "com.sleepycat.bind.tuple.LongBinding", "txstore.scratchpad.kvs.tests.data.MicroTupleBinding");
			TupleBinding t2KB = t1KB;
			TupleBinding t2DB = t1DB;
			config.putTableInfo("T3", new KVAbstractExecution("T3", true), "com.sleepycat.bind.tuple.LongBinding", "txstore.scratchpad.kvs.tests.data.MicroTupleBinding");
			TupleBinding t3KB = t1KB;
			TupleBinding t3DB = t1DB;
			config.putTableInfo("T4", new KVAbstractExecution("T4", false), "com.sleepycat.bind.tuple.LongBinding", "txstore.scratchpad.kvs.tests.data.MicroTupleBinding");
			TupleBinding t4KB = t1KB;
			TupleBinding t4DB = t1DB;
						
			long n = (new java.util.Date().getTime() / 1000) % 100000;
			
			KVScratchpad db = new KVScratchpad( config);
			
			KVPutResult ru;
			KVGetResult rq;
			
			db.beginTransaction( new ProxyTxnId(0,0,0));

			ru = (KVPutResult)db.execute( KVOperation.createPutOperation("T1", new Long(n), new MicroTuple(n , n%10, 2, 3, "S" + (n%100)), t1KB, t1DB));
			Debug.println( "result = " + ru);

			ru = (KVPutResult)db.execute( KVOperation.createPutOperation("T1", new Long(n+1), new MicroTuple(n+1 , (n+1)%10, 2, 3, "S" + ((n+1)%100)), t1KB, t1DB));
			Debug.println( "result = " + ru);

			ru = (KVPutResult)db.execute( KVOperation.createPutOperation("T2", new Long(n), new MicroTuple(n , n%5, 2, 3, "/" + (n%100)), t2KB, t2DB));
			Debug.println( "result = " + ru);

			rq = (KVGetResult)db.execute( KVOperation.createGetOperation("T1", new Long(n), t1KB));
			Debug.println( "query result = \n" + rq);
			

			ReadWriteSet rwset = db.complete();
			Debug.println( "complete = " + rwset);
			
			long []dcs = { 1, 2};
			LogicalClock lc = new LogicalClock( dcs, 1);
			TimeStamp ts = new TimeStamp( 1, n);
			
			db.commit( lc, ts);

/*			db.beginTransaction( new ProxyTxnId(0,0,1));
			rq = (DBSelectResult)db.execute( new DBOperation( "select * from t1;"));
			Debug.println( "query result = \n" + rq);
			rq = (DBSelectResult)db.execute( new DBOperation( "select * from t2;"));
			Debug.println( "query result = \n" + rq);
			db.abort();
*/
			

			Debug.println( "Test 1 completed with success");
			
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
}
