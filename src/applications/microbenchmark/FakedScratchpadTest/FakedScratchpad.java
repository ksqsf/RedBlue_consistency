package applications.microbenchmark.FakedScratchpadTest;

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import applications.simplestub.OperationList;
import applications.simplestub.Read;
import applications.simplestub.Write;

public class FakedScratchpad implements ScratchpadInterface {
	static int objectCount;
	static LogicalClock writeTimes[][];
	static Object locks[][];
	static int txnCount = 0;
	static LogicalClock futureClock = null;
	Vector<ReadSetEntry> reads;
	Vector<WriteSetEntry> writes;
	OperationLog opLog;
	Operation myOp;
	
	Object initLock = new Object();
	
    static void setSharedVariables(int dcCount, int objCount){
        if (writeTimes == null) {
            objectCount = objCount;
            writeTimes = new LogicalClock[2][];
            locks = new Object[2][];
            for (int j = 0; j < 2; j++) {
                writeTimes[j] = new LogicalClock[objCount];
                locks[j] = new Object[objCount];
                for (int i = 0; i < writeTimes[j].length; i++) {
                    writeTimes[j][i] = new LogicalClock(dcCount);
                    locks[j][i] = writeTimes[j][i];
                }
            }
           
        }
    }
	
	public FakedScratchpad(int dcCount) {
	}

	public void beginTransaction(ProxyTxnId txnId) {
		reads = new Vector<ReadSetEntry>();
		writes = new Vector<WriteSetEntry>();
	}

	public ReadSet getReadSet() {
		return new ReadSet(reads);
	}

	public WriteSet getWriteSet() {
		return new WriteSet(writes);
	}

    public Result execute(Operation op)throws ScratchpadException{ 
	myOp = op;
	OperationList ol = new OperationList(op.getOperation(), 0);
  	for (int i = 0; i < ol.getReads().size(); i++){
            Read r = ol.getReads().elementAt(i);
  	    reads.add(new ReadSetEntry(Long.toString(r.getId())+r.getColor()*objectCount,
				       r.getColor(),
				       writeTimes[r.getColor()][r.getId()]));
        }
	for (int i = 0; i < ol.getWrites().size(); i++){
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(Long.toString(w.getId())+w.getColor()*objectCount,
                                         w.getColor(),0));    
        }
	return new Result(op.getOperation(), new byte[] {0,0});
    }

	public void abort() throws ScratchpadException {
	}

    public OperationLog commit(LogicalClock lc, TimeStamp ts) throws ScratchpadException{
    	//System.out.println("try to commit " + lc);
        for (int i = 0; i <writes.size(); i++){
            WriteSetEntry w = writes.elementAt(i);
            synchronized(locks[w.getColor()][(int) (Long.parseLong(w.getObjectId())-w.getColor()*objectCount-Integer.MAX_VALUE)]){
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (Long.parseLong(w.getObjectId())-w.getColor()*objectCount-Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (Long.parseLong(w.getObjectId())-w.getColor()*objectCount-Integer.MAX_VALUE)] = lc2;
                //System.out.println("object id: " + w.getObjectId() + " write time: " + lc2 + "\n");
            }
        }
	return new OperationLog(myOp.getOperation());
    }

    static int nulloplogcount = 0;
    public void finalize(LogicalClock lc, TimeStamp ts) throws ScratchpadException {
        if (opLog.getOperationLogBytes().length == 0){
            nulloplogcount++;
            if (nulloplogcount % 100000==0)
                System.out.println("lots of null oplogs");
            return;
        }
            OperationList ol = new OperationList(opLog.getOperationLogBytes(), 0);
        for (int i = 0; i < ol.getWrites().size(); i++) {
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(Long.toString(w.getId()) + w.getColor() * objectCount,
                    w.getColor(),0));
        }
        for (int i = 0; i < writes.size(); i++) {
            WriteSetEntry w = writes.elementAt(i);
            synchronized (locks[w.getColor()][(int) (Long.parseLong(w.getObjectId()) - w.getColor() * objectCount-Integer.MAX_VALUE)]) {
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (Long.parseLong(w.getObjectId())-w.getColor()*objectCount-Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (Long.parseLong(w.getObjectId())-w.getColor()*objectCount-Integer.MAX_VALUE)] = lc2;        
                }
        }
    }

	public void applyOperationLog(OperationLog opLog, LogicalClock lc,
			TimeStamp ts) {

	}

	public void applyOperationLog(OperationLog opLog) {
		this.opLog = opLog;
	}

	public ReadWriteSet complete() {
		return new ReadWriteSet(getReadSet(), getWriteSet());
	}

	public OperationLog getOperationLog() {
		return null;
	}

    public void dumpWriteTimes(){
	for (int i = 0; i < objectCount; i++){
	    System.out.print(i+":"+writeTimes[0][i]+"\t");
	    if (i %5 == 4)
		System.out.println();
	}
        for (int i = 0; i < objectCount; i++){
	    System.out.print(i+":"+writeTimes[1][i]+"\t");
	    if (i %5 == 4)
		System.out.println();
	}
    }
    
    public ReadWriteSet getReadWriteSet(Operation op){
        ReadWriteSet rws;
        OperationList ol = new OperationList(op.getOperation(), 0);
	for (int i = 0; i < ol.getReads().size(); i++){
          Read r = ol.getReads().elementAt(i);
	    reads.add(new ReadSetEntry(Long.toString(r.getId())+r.getColor()*objectCount,
				       r.getColor(),
				       writeTimes[r.getColor()][r.getId()]));
      }
	for (int i = 0; i < ol.getWrites().size(); i++){
          Write w = ol.getWrites().elementAt(i);
          writes.add(new WriteSetEntry(Long.toString(w.getId())+w.getColor()*objectCount,
                                       w.getColor(),0));    
      }
        return new ReadWriteSet(getReadSet(), getWriteSet());
    }

	public void commitShadowOP(Operation op, LogicalClock lc, TimeStamp ts)
			throws ScratchpadException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ResultSet executeOrig(Operation op) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
