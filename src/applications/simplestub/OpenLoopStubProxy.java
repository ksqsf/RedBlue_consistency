package applications.simplestub;

import util.Debug;

import txstore.util.ProxyTxnId;
import txstore.proxy.OpenLoopProxyInterface;
import txstore.proxy.OpenLoopProxy;
import txstore.proxy.OpenLoopApplicationInterface;

import txstore.util.Operation;
import txstore.util.Result;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

import java.util.Hashtable;

public class OpenLoopStubProxy implements OpenLoopApplicationInterface {

    long totalCommitted = 0;
    long totalRequests;
    long outstandingRequests = 0;
    int objectCount;
    long totalSent = 0;
    double blueRate;
    Hashtable<Long, txnrecord> records;
    Hashtable<ProxyTxnId, Long> idmapping;
    long totallatency = 0;
    long globalStartTime = 0;
  

    public OpenLoopStubProxy(long totalReqs,
            long out,
            int objs,
            double blue) {
        totalRequests = totalReqs;
        objectCount = objs;
        outstandingRequests = out;
        blueRate = blue;
        totalCommitted = 0;
        if (totalCommitted > -40000) {
            totalCommitted = 0;
        }
        totalSent = totalCommitted;
        records = new Hashtable<Long, txnrecord>();
        idmapping = new Hashtable<ProxyTxnId, Long>();
    }
    OpenLoopProxy proxy;

    public void setProxy(OpenLoopProxy p) {
        proxy = p;
    }

    public int selectStorageServer(Operation op) {
        return 0;
    }

    public int selectStorageServer(byte[] op) {
        return 0;
    }

    public void returnResult(ProxyTxnId txnid,
            byte[] res) {

        proxy.commit(txnid);
    }

    public void transactionStarted(ProxyTxnId txnid) {

        boolean success = false;
        long i = (long) txnid.getCount();
        OperationList opList = new OperationList();
        for (int k = 0; k < 4; k++) {
            int id = (int) ((i * k + i + i / (k + 1))) % objectCount;
            if (id < 0) {
                id = -id;
            }
            int color = Math.random() < blueRate ? 1 : 0;
            //  System.out.println("writing; "+id+":"+color);

            opList.addWrite(new Write(id, color));
            id = (int) ((i * i + k * k + i / (k + 1))) % objectCount;
            if (id < 0) {
                id = -id;
            }
            color = Math.random() < blueRate ? 1 : 0;
            //  System.out.println("reading: "+id+":"+color);
            opList.addRead(new Read(id, color));
        }
//        System.out.println(opList.getWrites());
        //       System.out.println(opList.getReads());
        byte[] tmp = new byte[opList.getByteSize()];
        opList.getBytes(tmp, 0);
        proxy.execute(tmp, txnid);

    }
    long abortcount = 0;
    Object abort = new Object();

    public void transactionAborted(ProxyTxnId id) {
        synchronized (abort) {
            abortcount++;
        }
         

        // remove teh old mapping from id to the transaction and
        // add a new mapping for the new id to the transaction
        Long l = idmapping.get(id);
        idmapping.remove(id);
        ProxyTxnId pid = proxy.beginTxn();
 //       System.out.println("ABORT "+abortcount+" "+totalCommitted+" "+id+"->"+pid);
        idmapping.put(pid, l);

    }
    Object finishing = new Object();

    public void transactionCommitted(ProxyTxnId id) {
        txnrecord r = null;
        try {
            r = records.get(idmapping.get(id));
        } catch (Exception e) {
            try {
                System.out.println(id);
                System.out.println(idmapping.get(id));
                System.out.println(records.get(idmapping.get(id)));
                System.exit(-1);
            } finally {
                while (idmapping.get(id) == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ef) {;
                    }
                    System.out.println("slept on" + id);
                }
            }

        }
        synchronized (finishing) {
            totalCommitted++;
            long endTime = System.nanoTime();
            r.setEnd(endTime);
            if (totalCommitted == 1)
                globalStartTime = r.start;
            totallatency += r.latency();
//            if (totalCommitted == totalRequests) {
//                long total = endTime - startTime;
//                System.out.println(" totalRequests       " + totalRequests
//                        + " totalTime  (ns)     " + total
//                        + " throughput (op/s)   " + (double) ((double) totalRequests / (double) total * (double) 1000000 * (double) 1000)
//                        + " totallatency (ns)   " + totallatency
//                        + " latency    (ms/req) " + (double) ((double) totallatency / (double) totalRequests / (double) 1000000)
//                        + " abort count         " + abortcount);
//
//                
//            }
            
            if (totalSent >= totalRequests
                    && totalSent == totalCommitted) {
                System.exit(-1);
            }
            if (totalCommitted % 10000 == 0  && totalCommitted < totalRequests) {
                if (totalCommitted == 0) {
                    startTime = endTime;
                    totallatency = 0;
                }

                long total = endTime - startTime;
                long globalTotal = endTime - globalStartTime;
                System.out.println(" totalRequests       " + totalCommitted
                        + " totalTime  (ns)     " + total
                        + " throughput (op/s)   " + (double) ((double) 10000        / (double) total * (double) 1000000 * (double) 1000)
                        + " totallatency (ns)   " + totallatency
                        + " latency    (ms/req) " + (double) ((double) totallatency / (double) 10000 / (double) 1000000)
                        + " abort count         " + abortcount 
                        + " total thpt (op/s)    " + (double)((double)totalCommitted / (double) globalTotal* (double) 1000000 * (double) 1000));
                startTime = endTime;
                totallatency = 0;
                abortcount = 0;
            }
        }

        
        // TODO
    //    System.out.println("COMMIT: "+id+" "+idmapping.get(id));
        records.remove(idmapping.get(id));
        idmapping.remove(id);
//        System.out.println(records.size()+" "+idmapping.size());
        if (totalCommitted <= totalRequests) {
            beginTransaction();
        }

    }
    Object starting = new Object();

    public void beginTransaction() {
        long sendId = 0;
        Long sendid = null;
        synchronized (starting) {
           
            sendId = totalSent++;
            sendid = new Long(sendId);
        }
        records.put(sendid, new txnrecord(sendId, System.nanoTime()));
        ProxyTxnId pid = proxy.beginTxn();
        idmapping.put(pid, sendid);
       //  System.out.println("starting "+totalSent+" "+pid);





    }
    static public long startTime = 0;

    public static void main(String arg[]) {
        if (arg.length != 9) {
            System.out.println("usage: StubOpenLoopProxy config.xml dcId proxyId opCount outstandingOps objectCount blueRate threads smallmsgs");
            System.exit(0);
        }

        OpenLoopStubProxy olsp = new OpenLoopStubProxy(Long.parseLong(arg[3]), Long.parseLong(arg[4]), Integer.parseInt(arg[5]), Double.parseDouble(arg[6]));
        OpenLoopProxy imp = new OpenLoopProxy(arg[0], Integer.parseInt(arg[1]), Integer.parseInt(arg[2]), olsp);
        olsp.setProxy(imp);

        int objectCount = Integer.parseInt(arg[4]);

        boolean smallMsgs = Boolean.parseBoolean(arg[8]);

        // set up the networking for outgoing messages
        NettyTCPSender sendNet = new NettyTCPSender();
        imp.setSender(sendNet);
        sendNet.setTCPNoDelay(smallMsgs);

         int threadCount = Integer.parseInt(arg[7]);
        // set up the networking for incoming messages
        // first, create the pipe from the network to the proxy
        ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, threadCount);
        // then create the actual network
       
        NettyTCPReceiver rcv = new NettyTCPReceiver(
                imp.getMembership().getMe().getInetSocketAddress(), ptnq, 2);

        startTime = System.nanoTime();

        for (int i = 0; i < Long.parseLong(arg[4]); i++) {
            olsp.beginTransaction();
            try{
            Thread.currentThread().wait(10);
            }catch(Exception e){}
        }


    }
}

class txnrecord {

    long start;
    long end;
    long id;

    public txnrecord(long id, long start) {
        this.start = start;
        this.id = id;
    }

    public void setEnd(long e) {
        end = e;
    }

    public long latency() {
        return end - start;
    }

   
    public String toString() {
        return id + " (" + start + "->" + end + ") " + latency();
    }
}