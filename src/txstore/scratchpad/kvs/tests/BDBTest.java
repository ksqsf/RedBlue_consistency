package txstore.scratchpad.kvs.tests;

import com.sleepycat.db.*;
import com.sleepycat.bind.tuple.*;
import java.io.*;

import txstore.scratchpad.kvs.tests.data.MicroTuple;
import txstore.scratchpad.kvs.tests.data.MicroTupleBinding;


class LScratchpadTuple<T>
{
	public boolean del;
	public int ts;
	public String clock;
	public T object;
	public LScratchpadTuple() {
	}
	public LScratchpadTuple(boolean del, int ts, String clock, T object) {
		super();
		this.del = del;
		this.ts = ts;
		this.clock = clock;
		this.object = object;
	}
	public boolean isDel() {
		return del;
	}
	public void setDel(boolean del) {
		this.del = del;
	}
	public int getTs() {
		return ts;
	}
	public void setTs(int ts) {
		this.ts = ts;
	}
	public String getClock() {
		return clock;
	}
	public void setClock(String clock) {
		this.clock = clock;
	}
	public T getObject() {
		return object;
	}
	public void setObject(T object) {
		this.object = object;
	}
}


class LScratchpadTupleBinding<T,TBind extends TupleBinding<T>> extends TupleBinding<LScratchpadTuple<T>> {
	TBind dataBind;
	
	public LScratchpadTupleBinding( TBind bind) {
		this.dataBind = bind;
	}

    public void objectToEntry(LScratchpadTuple<T> myData, TupleOutput to) {
        to.writeBoolean(myData.isDel());
        to.writeInt(myData.getTs());
        to.writeString(myData.getClock());
        dataBind.objectToEntry(myData.getObject(), to);
    }

    public LScratchpadTuple<T> entryToObject(TupleInput ti) {
    	LScratchpadTuple<T> myData = new LScratchpadTuple<T>();
    	myData.setDel(ti.readBoolean());
    	myData.setTs(ti.readInt());
    	myData.setClock(ti.readString());
    	myData.setObject(dataBind.entryToObject(ti));
        return myData;
    }
} 

public class BDBTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Database myDB1 = null;
		Database myDB2 = null;
		Environment myEnv = null;

		try {
			new File("testdb").mkdir();
		    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
		    myEnvConfig.setInitializeCache(true);
		    myEnvConfig.setInitializeLocking(true);
		    myEnvConfig.setInitializeLogging(true);
		    myEnvConfig.setTransactional(true);
		    myEnvConfig.setMultiversion(true);
		    myEnvConfig.setAllowCreate(true);

		    myEnv = new Environment(new File("testdb"),
		                              myEnvConfig);

		    // Open the database.
		    DatabaseConfig dbConfig = new DatabaseConfig();
		    dbConfig.setTransactional(true);
		    dbConfig.setAllowCreate(true);
		    dbConfig.setType(DatabaseType.BTREE);
		    
		    myDB1 = myEnv.openDatabase(null,               // txn handle
                    "db1",   // db file name
                    null,             // db name
                    dbConfig);
		    LongBinding db1KB = new LongBinding();
		    MicroTupleBinding db1TB = new MicroTupleBinding();
		    LScratchpadTupleBinding<MicroTuple,MicroTupleBinding> db1DB = new LScratchpadTupleBinding<MicroTuple,MicroTupleBinding>(db1TB);
		    
		    myDB2 = myEnv.openDatabase(null,               // txn handle
                    "db2",   // db file name
                    null,             // db name
                    dbConfig);
		    LongBinding db2KB = new LongBinding();
		    MicroTupleBinding db2TB = new MicroTupleBinding();
		    LScratchpadTupleBinding<MicroTuple,MicroTupleBinding> db2DB = new LScratchpadTupleBinding<MicroTuple,MicroTupleBinding>(db1TB);


		    TransactionConfig txnConfig = new TransactionConfig();
		    txnConfig.setSnapshot(true);
		    
		    Transaction txn = myEnv.beginTransaction(null, txnConfig);
		    
		    MicroTuple mt = new MicroTuple( 10, 10, 10, 10, "cheng"); 
		    DatabaseEntry myDB1Key = new DatabaseEntry();
		    db1KB.objectToEntry( mt.a, myDB1Key);
		    
		    DatabaseEntry myDB1Data = new DatabaseEntry();
		    LScratchpadTuple<MicroTuple> spMt = new LScratchpadTuple<MicroTuple>( false, 1, "0-0", mt);
		    db1DB.objectToEntry( spMt, myDB1Data);
		    
		    myDB1.put( txn, myDB1Key, myDB1Data);
		    		    
		    txn.commit();

		    myDB1.close();
		    myDB2.close();
		    myEnv.close();
		} catch (Exception dbe) {
			dbe.printStackTrace();
		} 


	}

}
