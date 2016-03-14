/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package applications.versionzero;

import txstore.util.Result;
import txstore.util.Operation;
import txstore.baseserver.BaseUser;

import java.util.Random;

/**
 *
 * @author aclement
 */
public class ClientApplication implements txstore.baseserver.UserApplication {

    int opTotal;
    int count;
    int objects;
    float blue;
    int perTrans;  // numer of individual operations sent at one time;
    int[][] operations; // the different possible operations
    BaseUser proxy;
    Random rand;
    long SEED = 7337;
    
    // number of operations to issue, number of objects (of both colors), rate of blue operations,
    // number of microops per operation, selectable operations.

    public ClientApplication(int opCount, int objCount, int[] operations){
        this(opCount, objCount, operations, operations);
    }
    public ClientApplication(int opCount, int objCount, float blueRate, int opSize,
                int[] operations){
        this(opCount, objCount, blueRate, opSize, operations, operations);
    }
    public ClientApplication(int opCount, int objCount,
                int[] redoperations, int[] blueoperations){
        this(opCount, objCount, 0, 0, redoperations, blueoperations);
    }
    public ClientApplication(int opCount, int objCount, float blueRate, int opSize,
                int[] redoperations, int[] blueoperations){
        opTotal = opCount;
        count = 0;
        objects = objCount;
        blue = blueRate;
        rand = new Random(SEED);
        perTrans = opSize;
        this.operations = new int[2][];
        this.operations[0] = redoperations;
        this.operations[1] = blueoperations;
    }
    
    public void registerProxy(BaseUser b)
    {
        proxy = b;
    
    }    
    public void go(){
        if (count < opTotal){
            System.out.println("select elements to write. We want to label"
                    + " the objects that we will write and ensure they"
                    + "are sorted");
            OperationList ol = new OperationList();
            for (int i = 0; i < perTrans; i++){
                int objId = rand.nextInt() % objects;
                int color = 0;
                if (rand.nextFloat() < blue)
                    color = 1;
                int op = rand.nextInt() % operations[color].length;
                ol.addWrite(new Write(objId, color, op));
            }
            byte[] tmp = new byte[ol.getByteSize()];
            ol.getBytes(tmp, 0);
            Operation op = new Operation(tmp);
            System.out.println("Perform logging of operation start here");
            proxy.execute(op);
        }
        
    }
    
    public void processResult(Result res){
       
        System.out.println("Perform logging of operation finish here");
        count++;
        go();
   } 
}
