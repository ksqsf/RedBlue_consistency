package applications.microbenchmark.MemoryKVScratchpadTest;

import util.Debug;

import txstore.util.TimeStamp;
import txstore.util.WriteSet;
import txstore.util.ReadWriteSet;
import txstore.util.Result;
import txstore.util.Operation;
import txstore.util.LogicalClock;
import txstore.util.ProxyTxnId;
import txstore.util.ReadSet;
import txstore.util.OperationLog;
import txstore.util.ReadSetEntry;
import txstore.util.WriteSetEntry;

import txstore.scratchpad.ScratchpadInterface;
import txstore.scratchpad.ScratchpadException;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import applications.simplestub.KVWrite;
import applications.simplestub.OperationList;
import applications.simplestub.Read;
import applications.simplestub.Write;

public class FakedMemKVDirectScratchpad extends UnicastRemoteObject implements RemoteScratchpad {
	static int objectCount;
	static List<Value> database[][];
//	static LogicalClock writeTimes[][];
//	static Object locks[][];
	static int txnCount = 0;
//	static LogicalClock futureClock = null;
	static long databaseVersion;
	List<KVWrite> txWrites;
	Map<Integer,Long> cache;
	long txVersion;
	Operation myOp;
	
	Object initLock = new Object();
	
    static void setSharedVariables(int dcCount, int objCount){
        if( database == null) {
            objectCount = objCount;
            database = new LinkedList[2][];
            for (int j = 0; j < 2; j++) {
            	database[j] = new LinkedList[objCount];
                for (int i = 0; i < database[j].length; i++) {
                	database[j][i] = new LinkedList<Value>();
                	database[j][i].add( new Value( new LogicalClock(dcCount), -1, 0));
                }
            }
        }
    }
	
	public FakedMemKVDirectScratchpad(int dcCount) throws RemoteException {
		txWrites = new ArrayList<KVWrite>();
        cache = new TreeMap<Integer,Long>();
	}
	
	public void ping() throws RemoteException {
		
	}

	public void reset() throws RemoteException {
		txWrites = new ArrayList<KVWrite>();
        cache = new TreeMap<Integer,Long>();
	}

	
	protected static synchronized long getDatabaseVersion() {
		return databaseVersion;
	}
	protected static synchronized long incDatabaseVersion() {
		return databaseVersion++;
	}

	public void beginTransaction(MProxyTxnId txnId) throws RemoteException{
        cache.clear();
        txWrites.clear();
		txVersion = getDatabaseVersion();
	}

	public ReadSet getReadSet() {
		return new ReadSet(new Vector<ReadSetEntry>());
	}

	public WriteSet getWriteSet() {
		return new WriteSet(new Vector<WriteSetEntry>());
	}
	
	protected static Value getDatabaseValue( int color, int id, long version) {
		long dbVersion = getDatabaseVersion() - 1000;
		List<Value> lst = database[color][id];
		synchronized( lst) {
			Iterator<Value> it = lst.iterator();
			while( it.hasNext()) {
				Value v = it.next();
				if( v.version <= version) {
//					while( it.hasNext()) {
//						Value v2 = it.next();
//						if( v2.version > 0 && v2.version < dbVersion)
//							it.remove();
//					}
					return v;
				}
			}
		}
		throw new RuntimeException( "version not found: not expected");
	}

	protected static void putDatabaseValue( int color, int id, long val, LogicalClock lc, long version) {
		List<Value> lst = database[color][id];
		synchronized( lst) {
			lst.add(0, new Value( lc, version, val));
		}
	}

	protected static void putLWWDatabaseValue( int color, int id, long val, LogicalClock lc, long version) {
		List<Value> lst = database[color][id];
		synchronized( lst) {
			Value v = lst.get(0);
			
			LogicalClock newLc = lc.maxClock(v.lc);
			if( v.lc.strictLessThan(lc))
				lst.add(0, new Value( newLc, version, val));
		}
	}
				
	public Result execute(Operation op) throws RemoteException{ 
		myOp = op;
	OperationList ol = (op instanceof OpListOperation) ? ((OpListOperation)op).list : 
						new OperationList(op.getOperation(), 0);
  	for (int i = 0; i < ol.getReads().size(); i++){
            Read r = ol.getReads().elementAt(i);
            int eid = r.getId()+r.getColor()*objectCount;
    		Long l = cache.get( eid);
    		if( l != null)
    			continue;
            Value v = getDatabaseValue(r.getColor(),r.getId(),txVersion);
        }
	for (int i = 0; i < ol.getWrites().size(); i++){
            KVWrite w = (KVWrite)ol.getWrites().elementAt(i);
            int eid = w.getId()+w.getColor()*objectCount;
            txWrites.add(w);
            cache.put( eid, w.getVal());
        }
	return new Result(op.getOperation(), new byte[] {0,0});
    }

	public void abort() throws  RemoteException {
	}

    public boolean commit(LogicalClock lc, TimeStamp ts) throws RemoteException {
    	//System.out.println("try to commit " + lc);
    	long version = incDatabaseVersion();
    	Iterator<KVWrite> it = txWrites.iterator();
    	while( it.hasNext()) {
    		KVWrite w = it.next();
    		putLWWDatabaseValue(w.getColor(),w.getId(),w.getVal(),lc,version);
    	}
/*        for (int i = 0; i <writes.size(); i++){
            WriteSetEntry w = writes.elementAt(i);
            synchronized(locks[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount-Integer.MAX_VALUE)]){
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount-Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount-Integer.MAX_VALUE)] = lc2;
                //System.out.println("object id: " + w.getObjectId() + " write time: " + lc2 + "\n");
            }
        }
*/	return true;
    }

    static int nulloplogcount = 0;
    public void finalize(LogicalClock lc, TimeStamp ts) throws ScratchpadException {
        	long version = incDatabaseVersion();
        	Iterator<KVWrite> it = txWrites.iterator();
        	while( it.hasNext()) {
        		KVWrite w = (KVWrite)it.next();
        		putLWWDatabaseValue(w.getColor(),w.getId(),w.getVal(),lc,version);
        	}
/*        for (int i = 0; i < ol.getWrites().size(); i++) {
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(w.getId() + w.getColor() * objectCount,
                    w.getColor()));
        }
        for (int i = 0; i < writes.size(); i++) {
            WriteSetEntry w = writes.elementAt(i);
            synchronized (locks[w.getColor()][(int) (w.getObjectId() - w.getColor() * objectCount-Integer.MAX_VALUE)]) {
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount-Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount-Integer.MAX_VALUE)] = lc2;        
                }
        }
*/    }

	public void applyOperationLog(OperationLog opLog, LogicalClock lc,
			TimeStamp ts) {

	}

	public void applyOperationLog(OperationLog opLog) {
	}

	public ReadWriteSet complete() {
		return new ReadWriteSet(getReadSet(), getWriteSet());
	}

	public OperationLog getOperationLog() {
		return null;
	}

    public void dumpWriteTimes(){
	for (int i = 0; i < objectCount; i++){
	    System.out.print(i+":"+database[0][i].get(0).lc+"\t");
	    if (i %5 == 4)
		System.out.println();
	}
        for (int i = 0; i < objectCount; i++){
	    System.out.print(i+":"+database[1][i].get(0).lc+"\t");
	    if (i %5 == 4)
		System.out.println();
	}
    }
    
/*    public ReadWriteSet getReadWriteSet(Operation op){
        ReadWriteSet rws;
        OperationList ol = new OperationList(op.getOperation(), 0);
	for (int i = 0; i < ol.getReads().size(); i++){
          Read r = ol.getReads().elementAt(i);
	    reads.add(new ReadSetEntry(r.getId()+r.getColor()*objectCount,
				       r.getColor(),
				       writeTimes[r.getColor()][r.getId()]));
      }
	for (int i = 0; i < ol.getWrites().size(); i++){
          KVWrite w = (KVWrite)ol.getWrites().elementAt(i);
          writes.add(new WriteSetEntry(w.getId()+w.getColor()*objectCount,
                                       w.getColor()));    
      }
        return new ReadWriteSet(getReadSet(), getWriteSet());
    }
*/
}

