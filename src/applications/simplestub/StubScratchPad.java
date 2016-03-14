package applications.simplestub;
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

import txstore.storageshim.StorageApplication;

import txstore.scratchpad.ScratchpadInterface;
import txstore.scratchpad.ScratchpadException;

import java.util.Vector;

public class StubScratchPad implements ScratchpadInterface, StorageApplication{


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
    
    public StubScratchPad(int dcCount, int objCount){
    }
        
        
    public void beginTransaction(ProxyTxnId txnId){
	reads = new Vector<ReadSetEntry>();
	writes = new Vector<WriteSetEntry>();
    }


    public ReadSet getReadSet(){
	return new ReadSet(reads);
    }
    
    public WriteSet getWriteSet(){
	return new WriteSet(writes);
    }

    public Result execute(Operation op)throws ScratchpadException{ 
	myOp = op;
	OperationList ol = new OperationList(op.getOperation(), 0);
  	for (int i = 0; i < ol.getReads().size(); i++){
            Read r = ol.getReads().elementAt(i);
            
  	    reads.add(new ReadSetEntry(r.getId()+r.getColor()*objectCount,
				       r.getColor(),
				       writeTimes[r.getColor()][r.getId()]));
        }
	for (int i = 0; i < ol.getWrites().size(); i++){
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(w.getId()+w.getColor()*objectCount,
                                         w.getColor(),0));    
        }
	return new Result(op.getOperation(), new byte[] {0,0});
    }

    public void abort() throws ScratchpadException{}

    public OperationLog commit(LogicalClock lc, TimeStamp ts) throws ScratchpadException{
//      System.out.println("committing: "+lc+" "+ts);
//        System.out.println("\t write count: "+writes.size());
        for (int i = 0; i <writes.size(); i++){
            WriteSetEntry w = writes.elementAt(i);
            synchronized(locks[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount - Integer.MAX_VALUE)]){
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount - Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount - Integer.MAX_VALUE)] = lc2;        
            }
//	    System.out.println("\t\tcomiting: "+writes.elementAt(i).getObjectId()+" at "+lc +" with "+ts);
        }
        
//	dumpWriteTimes();
	return new OperationLog(myOp.getOperation());
    }

    static int nulloplogcount = 0;
    public void finalize(LogicalClock lc, TimeStamp ts) throws ScratchpadException {
   //     System.out.println("finalizing" + lc + " " + ts);
        if (opLog.getOperationLogBytes().length == 0){
            nulloplogcount++;
            if (nulloplogcount % 100000==0)
                System.out.println("lots of null oplogs");
            return;
        }
            OperationList ol = new OperationList(opLog.getOperationLogBytes(), 0);

   //     System.out.println("\twrite count: " + writes.size());
        for (int i = 0; i < ol.getWrites().size(); i++) {
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(w.getId() + w.getColor() * objectCount,
                    w.getColor(),0));
        }
   //     System.out.println("\t\twritecount : "+writes.size());
        for (int i = 0; i < writes.size(); i++) {
            WriteSetEntry w = writes.elementAt(i);
            synchronized (locks[w.getColor()][(int) (w.getObjectId() - w.getColor() * objectCount - Integer.MAX_VALUE)]) {
                LogicalClock lc2 =
                            lc.maxClock(writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount- Integer.MAX_VALUE)]);
                writeTimes[w.getColor()][(int) (w.getObjectId()-w.getColor()*objectCount - Integer.MAX_VALUE)] = lc2;        
                }
//	    System.out.println("\t\tcomiting: "+writes.elementAt(i).getObjectId()+" at "+lc +" with "+ts);
        }

  //      dumpWriteTimes();
    }

      
//    public void applyOperationLog(OperationLog opLog, LogicalClock lc, TimeStamp ts){
//       applyOperationLog(opLog);
//       
//    }

    public void applyOperationLog(OperationLog opLog){
 //       System.out.println("applyoperationlog");
	this.opLog = opLog;
       
	
    }

    public ReadWriteSet complete(){
       return new ReadWriteSet(getReadSet(), getWriteSet());
    }

    public OperationLog getOperationLog(){
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
  	    reads.add(new ReadSetEntry(r.getId()+r.getColor()*objectCount,
				       r.getColor(),
				       writeTimes[r.getColor()][r.getId()]));
        }
	for (int i = 0; i < ol.getWrites().size(); i++){
            Write w = ol.getWrites().elementAt(i);
            writes.add(new WriteSetEntry(w.getId()+w.getColor()*objectCount,
                                         w.getColor(),0));    
        }
          return new ReadWriteSet(getReadSet(), getWriteSet());
      }

}