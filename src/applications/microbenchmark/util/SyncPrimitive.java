package applications.microbenchmark.util;
import util.Debug;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.lang.InterruptedException;
//import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class SyncPrimitive implements Watcher {

    static ZooKeeper zk = null;
    static Integer mutex;
    static String watchPath = "";

    String root;

    SyncPrimitive(String address) {

        if(zk == null){
            try {
                Debug.println("Starting ZK:" + (new Date()).toString());
                mutex = Integer.valueOf(-1);
                zk = new ZooKeeper(address, 10000, this);
                Debug.println("Finished starting ZK: " + zk);
            } /*catch (KeeperException e) {
                Debug.println("Keeper exception when starting new session: "
                    + e.toString());
                zk = null;
            }*/ catch (IOException e) {
                Debug.println(e.toString());
                zk = null;
            }
            //else mutex = new Integer(-1);
        }
    }

    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            //Debug.println("Process: " + event.getType() + " " + event.getPath());
            watchPath = event.getPath();
            mutex.notify();
        }
    }

    ZooKeeper getZK(){
        return zk;
    }

    void close() throws InterruptedException
    {
        if(zk != null){
                zk.close();
        }
    }

    /**
     * Barrier
     */
    static public class Barrier extends SyncPrimitive {
        int size;
        String name;

        /**
         * Barrier constructor
         *
         * @param address
         * @param name
         * @param size
         */
        Barrier(String address, String bname, String nname, int size) {
            super(address);
            this.root = bname;
            this.size = size;

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    } else {
                        if(zk.exists(root + "/ready", false) != null) zk.delete(root + "/ready", 0) ;
                        if(zk.exists(root + "/done", false) != null) zk.delete(root + "/done", 0) ;
                    }
                } catch (KeeperException e) {
                    //System.out
                    //        .println("Keeper exception when instantiating barrier: "
                    //                + e.toString());
                } catch (InterruptedException e) {
                    Debug.println("Interrupted exception");
                }
            }

            // My node name
            try {
                this.name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString() + nname);
            } catch (UnknownHostException e) {
                Debug.println(e.toString());
            }

        }

        /**
         * Join barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean enter() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                zk.exists(root + "/ready", true);
                zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
                List<String> list = zk.getChildren(root, false);

                if (list.size() < size) {
                    boolean diff = true;
                    while(diff){
                        //zk.exists(root + "/ready", true);
                        mutex.wait();
                        if(watchPath != null){
                            if(watchPath.compareTo(root + "/ready") == 0)
                                diff = false;
                        } else {
                            if(zk.exists(root + "/ready", false) != null)
                                diff = false;
                        }
                        //Debug.println("Received notification.");
                    }
                } else {
                    //Debug.println("Going to create ready node.");
                    zk.create(root + "/ready", new byte[0],
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            }

            return true;
        }

        /**
         * Wait until all reach barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean leave() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                zk.exists(root + "/done", true);
                zk.delete(root + "/" + name, 0);

                List<String> list = zk.getChildren(root, false);
                if (list.size() > 1) {
                    boolean diff = true;
                    while(diff){
                        mutex.wait();
                        if(watchPath != null){
                            if(watchPath.compareTo(root + "/done") == 0)
                                diff = false;
                        } else {
                            if(zk.exists(root + "/done", false) != null)
                                diff = false;
                        }
                    }
                } else {
                    zk.create(root + "/done", new byte[0],
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            }

            return true;
        }
    }

    /**
     * Barrier
     */
    static public class BarrierExp extends SyncPrimitive {
        int size;
        String name;

        /**
         * Barrier constructor
         *
         * @param address
         * @param name
         * @param size
         */
        BarrierExp(String address, String bname, String nname, int size) {
            super(address);
            this.root = bname;
            this.size = size;

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    } else {
                        if(zk.exists(root + "/zzz-ready", false) != null) zk.delete(root + "/ready", 0) ;
                        if(zk.exists(root + "/done", false) != null) zk.delete(root + "/done", 0) ;
                    }
                } catch (KeeperException e) {
                    //System.out
                    //        .println("Keeper exception when instantiating barrier: "
                    //                + e.toString());
                } catch (InterruptedException e) {
                    Debug.println("Interrupted exception");
                }
            }

            // My node name
            try {
                this.name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString() + nname);
            } catch (UnknownHostException e) {
                Debug.println(e.toString());
            }

        }

        /**
         * Join barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean enter() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                zk.exists(root + "/zzz-ready", true);
                zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
                List<String> list = zk.getChildren(root, false);

                if (list.size() < size) {
                    boolean diff = true;
                    while(diff){
                        //zk.exists(root + "/ready", true);
                        mutex.wait();
                        if(watchPath != null){
                            if(watchPath.compareTo(root + "/zzz-ready") == 0)
                                diff = false;
                        } else {
                            if(zk.exists(root + "/zzz-ready", false) != null)
                                diff = false;
                        }
                        //Debug.println("Received notification.");
                    }
                } else {
                    //Debug.println("Going to create ready node.");
                    zk.create(root + "/zzz-ready", new byte[0],
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                }
            }

            return true;
        }

        /**
         * Wait until all reach barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        boolean leave() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                Stat s = null;
                String low, high;

                while(true){
                    //Read the current list of nodes
                    List<String> list = zk.getChildren(root, false);

                    if(list.size() <= 1)
                            break;

                    low = list.get(list.size()-2);
                    high = list.get(0);
                    //Get highest
                    if(high.compareTo(this.name) >= 0){
                        //delete myself
                        if(high.compareTo(this.name) == 0){
                            zk.delete(root + "/" + this.name, 0);
                        }
                        // If I'm the lowest node, then leave
                        if(low.compareTo(this.name)==0) break;
                        // wait for the lowest node to disappear
                        s = zk.exists(root + "/" + low, true);
                    } else {
                        int i = 0;
                        int next = 0;
                        for(String str : list){
                            if(str.compareTo(name) == 0){
                                next = i;
                                break;
                            }
                            i++;
                        }
                        s = zk.exists(root + "/" + list.get((next==0)? 0 : (next - 1)), true);
                    }
                    if(s != null) mutex.wait();
                }
            }
            return true;
        }
    }

    /**
     * Barrier
     */
    static public class BarrierExp2 extends SyncPrimitive {
        int size;
        String name;
        static AtomicInteger readCounter = null;
        static AtomicInteger writeCounter = null;

        /**
         * Barrier constructor
         *
         * @param address
         * @param name
         * @param size
         */
        public BarrierExp2(String address, String bname, String nname, int size) {
            super(address);
            this.root = bname;
            this.size = size;

            if(readCounter == null) readCounter = new AtomicInteger();
            if(writeCounter == null) writeCounter = new AtomicInteger();

            // Create barrier node
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    readCounter.incrementAndGet();
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        writeCounter.incrementAndGet();
                    } else {
                        if(zk.exists(root + "/zzz-ready", false) != null){
                            zk.delete(root + "/zzz-ready", 0) ;
                            writeCounter.incrementAndGet();
                        }
                        if(zk.exists(root + "/done", false) != null){
                            zk.delete(root + "/done", 0) ;
                            writeCounter.incrementAndGet();
                        }
                        readCounter.addAndGet(2);
                    }
                } catch (KeeperException e) {
                    //System.out
                    //        .println("Keeper exception when instantiating barrier: "
                    //                + e.toString());
                } catch (InterruptedException e) {
                    Debug.println("Interrupted exception");
                }
            }

            // My node name
            try {
                this.name = new String(InetAddress.getLocalHost().getCanonicalHostName().toString() + nname);
            } catch (UnknownHostException e) {
                Debug.println(e.toString());
            }

        }

        /**
         * Join barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        public boolean enter() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                zk.exists(root + "/zzz-ready", true);
                zk.create(root + "/" + name, new byte[0], Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL);
                List<String> list = zk.getChildren(root, false);
                readCounter.addAndGet(2);
                writeCounter.incrementAndGet();

                if (list.size() < size) {
                    boolean diff = true;
                    while(diff){
                        mutex.wait();
                        if(zk.exists(root + "/zzz-ready", false) != null)
                                diff = false;
                        readCounter.incrementAndGet();
                        Debug.println("Received notification.");
                    }
                } else {
                    //Debug.println("Going to create ready node.");
                    try{
                        zk.create(root + "/zzz-ready", new byte[0],
                            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                        writeCounter.incrementAndGet();
                    } catch (KeeperException.NodeExistsException e){

                    }
                }
            }
            return true;
        }

        /**
         * Wait until all reach barrier
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */

        public boolean leave() throws KeeperException, InterruptedException{

            synchronized (mutex) {
                Stat s = null;
                String low, high;
                String watchingOn;

                while(true){
                    //Read the current list of nodes
                    List<String> list = zk.getChildren(root, false);
                    readCounter.incrementAndGet();

                    if(list.size() <= 1){
                        Debug.println("List size is less then 1 (" + root + "), " + list.size());
                        if(list.size() == 1){
                            if(this.name.compareTo(list.get(0)) == 0){
                                zk.delete(root + "/" + this.name, -1);
                                writeCounter.incrementAndGet();
                            }
                        }
                        break;
                    }
                    Collections.sort(list);
                    low = list.get(list.size() - 2);
                    high = list.get(0);
                    //Debug.println("Low: " + root + "/" + low + " High: " + high + " Last: " + list.get(list.size() - 1));
                    //Get highest

                    if(low.compareTo(this.name) == 0){
                        if(low.compareTo(high) == 0){
                            //Debug.println("Low == High");
                            zk.delete(root + "/" + this.name, -1);
                            writeCounter.incrementAndGet();

                            break;
                        }
                        //don't delete myself
                        // wait for the lowest node to disappear
                        s = zk.exists(root + "/" + high, true);
                        readCounter.incrementAndGet();

                        watchingOn = root + "/" + high;
                    } else {
                        //Debug.println("I'm not low");
                        zk.delete(root + "/" + this.name, -1);
                        s = zk.exists(root + "/" + low, true);
                        writeCounter.incrementAndGet();
                        readCounter.incrementAndGet();

                        watchingOn = root + "/" + low;
                    }
                    if(s != null){
                        boolean myWatch = false;
                        while(!myWatch){
                            mutex.wait();
                            if(watchPath.compareTo(watchingOn) == 0) myWatch = true;
                        }
                    }
                }
            }
            return true;
        }
    }

    /**
     * Producer-Consumer queue
     */
    static public class Queue extends SyncPrimitive {

        /**
         * Constructor of producer-consumer queue
         *
         * @param address
         * @param name
         */
        Queue(String address, String name) {
            super(address);
            this.root = name;
            // Create ZK node name
            if (zk != null) {
                try {
                    Stat s = zk.exists(root, false);
                    if (s == null) {
                        zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    }
                } catch (KeeperException e) {
                    System.out
                            .println("Keeper exception when instantiating queue: "
                                    + e.toString());
                } catch (InterruptedException e) {
                    Debug.println("Interrupted exception");
                }
            }
        }

        /**
         * Add element to the queue.
         *
         * @param i
         * @return
         */

        boolean produce(int i) throws KeeperException, InterruptedException{
            ByteBuffer b = ByteBuffer.allocate(4);
            byte[] value;

            // Add child with value i
            b.putInt(i);
            value = b.array();
            zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT_SEQUENTIAL);

            return true;
        }


        /**
         * Remove first element from the queue.
         *
         * @return
         * @throws KeeperException
         * @throws InterruptedException
         */
        int consume() throws KeeperException, InterruptedException{
            int retvalue = -1;
            Stat stat = null;

            // Get the first element available
            while (true) {
                synchronized (mutex) {
                    List<String> list = zk.getChildren(root, true);
                    if (list.size() == 0) {
                        Debug.println("Going to wait");
                        mutex.wait();
                    } else {
                        Integer min = new Integer(list.get(0).substring(7));
                        for(String s : list){
                            Integer tempValue = new Integer(s.substring(7));
                            //Debug.println("Temporary value: " + tempValue);
                            if(tempValue < min) min = tempValue;
                        }
                        Debug.println("Temporary value: " + root + "/element" + min);
                        byte[] b = zk.getData(root + "/element" + min,
                                    false, stat);
                        zk.delete(root + "/element" + min, 0);
                        ByteBuffer buffer = ByteBuffer.wrap(b);
                        retvalue = buffer.getInt();

                        return retvalue;
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
        if (args[0].equals("qTest"))
            queueTest(args);
        else
            barrierTest(args);

    }

    public static void queueTest(String args[]) {
        Queue q = new Queue(args[1], "/app1");

        Debug.println("Input: " + args[1]);
        int i;
        Integer max = new Integer(args[2]);

        if (args[3].equals("p")) {
            Debug.println("Producer");
            for (i = 0; i < max; i++)
                try{
                    q.produce(10 + i);
                } catch (KeeperException e){

                } catch (InterruptedException e){

                }
        } else {
            Debug.println("Consumer");

            for (i = 0; i < max; i++) {
                try{
                    int r = q.consume();
                    Debug.println("Item: " + r);
                } catch (KeeperException e){
                    i--;
                } catch (InterruptedException e){

                }
            }
        }
    }

    public static void barrierTest(String args[]) {
        //Barrier b = new Barrier(args[1], "/b1", "0", new Integer(args[2]));
        BarrierExp2 b = new BarrierExp2(args[1],"/b1","0",new Integer(args[2]));
        try{
            boolean flag = b.enter();
            Debug.println("Entered barrier: " + args[2]);
            if(!flag) Debug.println("Error when entering the barrier");
        } catch (KeeperException e){

        } catch (InterruptedException e){

        }

        // Generate random integer
        Random rand = new Random();
        int r = rand.nextInt(100);
        // Loop for rand iterations
        for (int i = 0; i < r; i++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }
        try{
            b.leave();
        } catch (KeeperException e){

        } catch (InterruptedException e){

        }
        Debug.println("Left barrier");
    }
}
