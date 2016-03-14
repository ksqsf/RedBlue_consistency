package applications.simplestub;

import util.Debug;

import txstore.util.ProxyTxnId;
import txstore.proxy.OpenLoopProxyInterface;
import txstore.proxy.SimpleOpenLoopProxy;
import txstore.proxy.OpenLoopApplicationInterface;

import txstore.util.Operation;
import txstore.util.Result;

import network.netty.NettyTCPSender;
import network.netty.NettyTCPReceiver;
import network.ParallelPassThroughNetworkQueue;

import java.util.Hashtable;

public class OpenLoopStubSimpleProxy implements OpenLoopApplicationInterface {

    long totalCommitted = 0;
    long totalRequests;
    long outstandingRequests = 0;
    int objectCount;
    long totalSent = 0;
    double blueRate;
    Hashtable<Long, txnrecord2> records;
    Hashtable<ProxyTxnId, Long> idmapping;
    long totallatency = 0;

    public OpenLoopStubSimpleProxy(long totalReqs,
            long out,
            int objs,
            double blue) {
        totalRequests = totalReqs;
        objectCount = objs;
        outstandingRequests = out;
        blueRate = blue;
        totalCommitted = 0;
        //        totalCommitted = -outstandingRequests * 10;
//        if (totalCommitted > -40000) {
//            totalCommitted = -40000;
//        }
        totalSent = totalCommitted;
        records = new Hashtable<Long, txnrecord2>();
        idmapping = new Hashtable<ProxyTxnId, Long>();
    }
    SimpleOpenLoopProxy proxy;

    public void setProxy(SimpleOpenLoopProxy p) {
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
        // System.out.println("ABORT "+abortcount+" "+totalCommitted);

        // remove teh old mapping from id to the transaction and
        // add a new mapping for the new id to the transaction
        Long l = idmapping.get(id);
        idmapping.remove(id);
        ProxyTxnId pid = proxy.beginTxn();
        idmapping.put(pid, l);

    }
    Object finishing = new Object();

    public static long globalStartTime = 0;
    
    public void transactionCommitted(ProxyTxnId id) {
        txnrecord2 r = null;
        try {
            r = records.get(idmapping.get(id));
        } catch (Exception e) {
            try {
                e.printStackTrace();
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
            //log r
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
//            }
            if (totalSent >= totalRequests
                    && totalSent == totalCommitted) {
                System.exit(-1);
            }
            if (totalCommitted % 10000 == 0  && totalCommitted < totalRequests) {
//                if (totalCommitted == 0) {
//                    startTime = endTime;
//                    totallatency = 0;
//                }

                long total = endTime - startTime;
                long globalTotal = endTime - globalStartTime;
                System.out.println(" totalRequests       " + totalCommitted
                        + " totalTime  (ns)     " + total
                        + " throughput (op/s)   " + (double) ((double) 10000        / (double) total * (double) 1000000 * (double) 1000)
                        + " totallatency (ns)   " + totallatency
                        + " latency    (ms/req) " + (double) ((double) totallatency / (double) 10000 / (double) 1000000)
                        + " abort count         " + abortcount 
                        + " global total  (s)   " + globalTotal
                        + " total thpt (op/s)   " + (double)((double)totalCommitted / (double) globalTotal* (double) 1000000 * (double) 1000));
                startTime = endTime;
                totallatency = 0;
                abortcount = 0;
                
            }
        }

        records.remove(idmapping.get(id));
        idmapping.remove(id);
        if (totalSent < totalRequests) {
            beginTransaction();
        }
    }
    Object starting = new Object();

    public void beginTransaction() {
        long sendId = 0;
        Long sendid = null;
        synchronized (starting) {
//            System.out.println("starting "+totalSent);
            sendId = totalSent++;
            sendid = new Long(sendId);
        }
        records.put(sendid, new txnrecord2(sendId, System.nanoTime()));
        ProxyTxnId pid = proxy.beginTxn();
        idmapping.put(pid, sendid);





    }
    static public long startTime = 0;

    public static void main(String arg[]) {
        if (arg.length != 9) {
            System.out.println("usage: StubOpenLoopProxy config.xml dcId proxyId opCount outstandingOps objectCount blueRate threads smallmsgs");
            System.exit(0);
        }

        OpenLoopStubSimpleProxy olsp = new OpenLoopStubSimpleProxy(Long.parseLong(arg[3]), Long.parseLong(arg[4]), Integer.parseInt(arg[5]), Double.parseDouble(arg[6]));
        SimpleOpenLoopProxy imp = new SimpleOpenLoopProxy(arg[0], Integer.parseInt(arg[1]), Integer.parseInt(arg[2]), olsp);
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

class txnrecord2 {

    long start;
    long end;
    long id;

    public txnrecord2(long id, long start) {
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