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

public class Test2
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

			KVScratchpad db1 = new KVScratchpad( config);
			
			KVPutResult ru;
			KVGetResult rq;
			
			db1.beginTransaction( new ProxyTxnId(0,0,0));

			MicroTuple tuple = new MicroTuple( 10, 10, 10, 10, "Ss");
			
			ru = (KVPutResult)db1.execute( KVOperation.createPutOperation("T1", 10L, tuple, t1KB, t1DB));
			Debug.println( "result = " + ru);

			rq = (KVGetResult)db1.execute( KVOperation.createGetOperation("T1", 10L, t1KB));
			Debug.println( "query result = \n" + rq);
			
			if( ! ((MicroTuple)rq.getData()).equals(tuple))
				Debug.println("ERROR : NOT EXPECTED VALUE ");
			
			ReadWriteSet rwset = db1.complete();
			Debug.println( "complete = " + rwset);
			
			long []dcs = { 1, 2};
			LogicalClock lc = new LogicalClock( dcs, 1);
			TimeStamp ts = new TimeStamp( 1, n);
			
			db1.commit( lc, ts);

			db1.beginTransaction( new ProxyTxnId(0,0,1));

			rq = (KVGetResult)db1.execute( KVOperation.createGetOperation("T1", 10L, t1KB));
			Debug.println( "query result = \n" + rq);
			
			if( ! ((MicroTuple)rq.getData()).equals(tuple))
				Debug.println("ERROR : NOT EXPECTED VALUE ");

			db1.abort();

			db1.beginTransaction( new ProxyTxnId(0,0,2));

			KVBooleanResult ru2 = (KVBooleanResult)db1.execute( ChangeStrInMicroTuple.createOperation( "T1", 10L, 11, t1KB));
			Debug.println( "update result = \n" + ru2);

			dcs = new long[]{ 1, 2};
			lc = new LogicalClock( dcs, 1);
			ts = new TimeStamp( 1, n+1);
			
			db1.commit( lc, ts);

			db1.beginTransaction( new ProxyTxnId(0,0,3));

			rq = (KVGetResult)db1.execute( KVOperation.createGetOperation("T1", 10L, t1KB));
			Debug.println( "query result = \n" + rq);
			
			db1.abort();
			
			db1.beginTransaction( new ProxyTxnId(0,0,4));

			ru2 = (KVBooleanResult)db1.execute( ChangeStrInMicroTuple.createOperation( "T1", 10L, 9, t1KB));
			Debug.println( "update result = \n" + ru2);

			dcs = new long[]{ 1, 2};
			lc = new LogicalClock( dcs, 1);
			ts = new TimeStamp( 1, n+1);
			
			db1.commit( lc, ts);

			db1.beginTransaction( new ProxyTxnId(0,0,5));

			rq = (KVGetResult)db1.execute( KVOperation.createGetOperation("T1", 10L, t1KB));
			Debug.println( "query result = \n" + rq);
			
			db1.abort();
			
			Debug.println( "Test 2 completed with success");
			
		} catch( Exception e) {
			e.printStackTrace();
		}
	}
}
