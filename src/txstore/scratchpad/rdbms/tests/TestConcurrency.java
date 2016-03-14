package txstore.scratchpad.rdbms.tests;

import txstore.scratchpad.ScratchpadConfig;
import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.resolution.LWWLockExecution;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBSingleOperation;
import txstore.scratchpad.rdbms.util.DBUpdateResult;
import txstore.util.LogicalClock;
import txstore.util.ProxyTxnId;
import txstore.util.ReadWriteSet;
import txstore.util.TimeStamp;
import util.Debug;

public class TestConcurrency {
	public class ScratchpadThread extends Thread{
		ScratchpadConfig config;
		DBScratchpad db ;
		public ScratchpadThread(ScratchpadConfig conf) throws ScratchpadException{
			config = conf;
			db = new DBScratchpad( config);
		}
		
		public void run(){
			int count = 500;
			while(count-->0){
				DBUpdateResult ru;
				DBSelectResult rq;
				synchronized(starting){
					txnId++;
					db.beginTransaction( new ProxyTxnId(0,0,txnId));
				}
	
				long n = (new java.util.Date().getTime() / 1000) % 100000;
				try {
	
					rq = (DBSelectResult)db.execute( new DBSingleOperation( "select * from t1 where a > 10000 limit 2;"));
					//Debug.println( "query result = \n" + rq);
					ReadWriteSet rwset = db.complete();
					//Debug.println( "complete = " + rwset);
					
					long []dcs = { 1, 2};
					LogicalClock lc = new LogicalClock( dcs, 1);
					TimeStamp ts = new TimeStamp( 1, n);
					if(count % 10 == 0){
						Thread.sleep(40);
					}
					db.commit( lc, ts);
				} catch (ScratchpadException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			
		}
	}
	
	Object starting = new Object();
	static int txnId = 0;
	
	public TestConcurrency(){
		
	}
	
	public static void main( String[] args) throws ScratchpadException, InterruptedException {
		ScratchpadConfig config = null;
		try {
				config = new ScratchpadConfig( "com.mysql.jdbc.Driver", "jdbc:mysql://139.19.131.147:50000/micro", "root", "123456", "txstore.scratchpad.rdbms.DBScratchpad");
				config.putPolicy("T1", new LWWLockExecution(false));
				config.putPolicy("T2", new AllOpsLockExecution(true));
			
		}catch( Exception e) {
			e.printStackTrace();
		}
		TestConcurrency obj = new TestConcurrency();
		
		//create thread here
		for (int i =0 ;i < 100; i ++){
			TestConcurrency.ScratchpadThread thd = obj.new ScratchpadThread(config);
			thd.start();
			if(i % 20 == 0){
				Thread.sleep(100);
			}
		}
		
	}

}
