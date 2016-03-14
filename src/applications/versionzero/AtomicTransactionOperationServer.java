/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package applications.versionzero;

import txstore.util.Result;
import txstore.util.Operation;

import txstore.lockingstorageshim.LockManager;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author aclement
 */
public class AtomicTransactionOperationServer implements txstore.baseserver.ServerApplication{

    ReentrantLock[] redLocks;
    int[] redObjects;
    ReentrantLock[] blueLocks;
    int[] blueObjects;
    ReentrantLock[][] locks;
    int[][] objects;
    
    
    LockManager lockManager;
    
    public AtomicTransactionOperationServer(int objects){
        lockManager = new LockManager(objects, objects);
        redLocks = new ReentrantLock[objects];
        redObjects = new int[objects];
        blueLocks = new ReentrantLock[objects];
        blueObjects = new int[objects];
        for (int i = 0; i < objects; i++){
            redLocks[i] = new ReentrantLock();
            blueLocks[i] = new ReentrantLock();
        }
        locks = new ReentrantLock[2][];
        locks[0] = redLocks;
        locks[1] = blueLocks;
        this.objects = new int[2][];
        this.objects[0] = redObjects;
        this.objects[1] = blueObjects;
    }
    
    
    public Result execute(Operation op){
        OperationList ol = new OperationList(op.getOperation(), 0);
      
        
	int objs[] = new int[ol.getSize()];
        int color[] = new int[objs.length];
        int ops[] = new int[objs.length];
        for (int i = 0; i < ol.getSize(); i++){
            Write w = ol.getWrite(0);
            objs[i] = w.getId();
            color[i] = w.getColor();
            ops[i] = w.getOp();
        }
        
        twoPhaseExecute(objs, color, ops, 0);
		      
        // currently sending back a silly (and empty) response.
        return new Result(op.getOperation(), 0);
    }
        
    
    /*
     *  Execute operations.  Pre-emptively acquire locks on Blue. 
     *  Do nothing for the red operations.
     *  Requires the array objects to be pre-sorted in order to avoid deadlock
     */
    private void twoPhaseExecute(int[] objects, int[] color, int[] op, int index){
        if (index == objects.length){
            System.out.println("execute everything now"
                    + "\t\tby execute, i mean log the operations to disk");
            innerExecute(objects, color, op);
        } else{
                if (lockManager.acquire(objects[index], color[index])){
                    twoPhaseExecute(objects, color, op, index+1);
                    lockManager.release(objects[index], color[index]);
                }else{
                    System.out.println("Must abort the transaction now! Lock"+
                                       " acquisition failed");
                    System.exit(-1);
                }
            }
            
    }
    
    private void innerExecute(int[] objects, int[] color, int[] op){
        for (int i = 0; i < objects.length; i++)
            applyOperation(objects[i], color[i], op[i]);
    }
    
    /*
     * Apply operations
     * Operation 0:  non-locked increment by one
     * Operation 1:  locked increment by one
     */  
    private void applyOperation(int object, int color, int op){
        switch(op){
            case OperationTag.INCREMENT: 
                    objects[color][object]+=1;
                    System.out.println("Object "+object+","+color+" "
                            + objects[color][object]);                    
                    return;
            case OperationTag.LOCKINGINCREMENT: 
                   locks[color][object].lock();
                        objects[color][object]+=1;
                        System.out.println("Object "+object+","+color+" "
                            + objects[color][object]);
                   locks[color][object].unlock();
                   return;
            default: System.out.println("Unknown operation: "+op);
                     System.exit(-1);
        }
        
        
    }
    
    
    public void replayLog(String logFile){
        System.out.println("read the log file from disk and replay the"
                + "operations in order to observe the periodic state");
        
    }
}

