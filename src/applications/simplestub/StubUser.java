/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package applications.simplestub;

import applications.microbenchmark.membership.Role;
/**
 *
 * @author aclement
 */
public class StubUser extends applications.microbenchmark.membership.MicroBaseNode{
    
     long completed;
     long started;
     int outstanding;
    long totalRequests;
    double blueRate;
    
    long starttimes[][];
    long endtimes[][];
    int period=0;
    boolean canstart = true;
           
    
    public StubUser(String membershipFile, int datacenter,
			int myId, long reqs, int outstanding,
                        double blue) {
        super(membershipFile, datacenter, Role.USER, myId);
        blueRate = blue;
        totalRequests = reqs;
        this.outstanding = outstanding;
        starttimes=new long[2][];
        endtimes=new long[2][];
        for (int i = 0; i < 2; i++){
            starttimes[i] = new long[outstanding];
            endtimes[i]=new long[outstanding];
        }
		
	
    }
    
    @Override
	public void handle(byte[] b) {
		System.out.println("must handle messages");
	}
    
    
  
    private void process(){//ResponseMsg rmsg){
        endtimes[period][1] = System.nanoTime();
        completed++;
        if (completed%(starttimes[0].length)==0){
            period = period-1;
            for(int i = 0;i < starttimes[period].length; i++)
                System.out.println("id "+starttimes[period][i]+" "+endtimes[period][i]);
            
        }
        if (completed>= totalRequests){
            canstart = false;
            return;
        }
            newTransaction();
        
        
    }
    
    public void jumpStart(){
        while (started < outstanding){
            newTransaction();
        }
        
    }
    
    private void newTransaction(){
        if (canstart){
            started++;
            System.out.println("create a new transaction based on "+
                               " the workload profiles");
            System.out.println("send new transaction");
            process();
        }
    }
    
    public static void main(String arg[]){
        if (arg.length != 5){
            System.out.println("usage: StubUser config.xml clientId dcId"+
                               " totalReqs outstandingReqs blueRate");
            System.exit(-1);
        
        }
        
        StubUser user = new StubUser(arg[0],  // config file
                                     Integer.parseInt(arg[1]), // clientId
                                     Integer.parseInt(arg[2]), // dcid
                                     Long.parseLong(arg[3]),   // total reqs
                                     Integer.parseInt(arg[4]), // outstanding
                                     Double.parseDouble(arg[5])); // blue rate
                
                
        // set up the stubuser networking.
        
        user.jumpStart();
        // create the objects
        
        // call jumpstart
    }
	
}
