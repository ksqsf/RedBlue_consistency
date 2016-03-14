package applications.microbenchmark.FakedScratchpadTest;

import util.Debug;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import txstore.proxy.OpenLoopProxy;
import txstore.proxy.OpenLoopApplicationInterface;
import txstore.util.ProxyTxnId;

import applications.microbenchmark.messages.MessageBase;
import applications.microbenchmark.messages.MessageFactory;
import applications.microbenchmark.messages.MessageTags;
import applications.microbenchmark.membership.Role;
import applications.microbenchmark.membership.MicroBaseNode;
import applications.microbenchmark.messages.TxnRepMessage;
import applications.microbenchmark.messages.TxnReqMessage;
import applications.simplestub.OperationList;

import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;
import network.ParallelPassThroughNetworkQueue;

public class OpenLoopFakedAppServer extends MicroBaseNode implements OpenLoopApplicationInterface {

    int dcId;
    int appServerId;
    MessageFactory mf;
    OpenLoopProxy imp;
    FakedWorkloadGenerator fwl;
    Hashtable<ProxyTxnId, txnrecord> records;
    Hashtable<ProxyTxnId, ProxyTxnId> idmapping;
    long totalSent = 0;

    public OpenLoopFakedAppServer(String file_name, int s1, int s2) {

        super(file_name, s1, Role.APPSERVER, s2);
        dcId = s1;
        appServerId = s2;


        mf = new MessageFactory();
        System.out.println("NEED A WORKLOAD GENERATOR HERE");
        System.exit(-1);
    }

    public void setProxy(OpenLoopProxy olp) {

        imp = olp;
    }
    Object _starting = new Object();

    public void issueTxn(TxnReqMessage msg) {
        // generate commands here
        boolean success = false;
        int color = msg.getColor();
        int userId = msg.getTxnId().getProxyId();
        int keySelection = msg.getKeySelection();
        int workloadType = msg.getWorkloadType();
        Debug.printf("this transaction is from user %d\n", userId);
        OperationList opList = fwl.generate_commands(color, keySelection, userId, workloadType);



        records.put(msg.getTxnId(),
                new txnrecord(msg.getTxnId(), System.nanoTime(), opList));
        ProxyTxnId pid = imp.beginTxn();
        idmapping.put(pid, msg.getTxnId());
    }

    public void transactionStarted(ProxyTxnId txnid) {
        txnrecord rec = records.get(idmapping.get(txnid));
        OperationList op = rec.nextOp();
        byte tmp[] = new byte[op.getByteSize()];
        op.getBytes(tmp, 0);
        imp.execute(tmp, txnid);
    }

    public void returnResult(ProxyTxnId txnid,
            byte[] res) {

        txnrecord rec = records.get(idmapping.get(txnid));
        rec.opIssued();
        OperationList op = rec.nextOp();
        if (op == null) {
            imp.commit(txnid);
            return;
        } else {
            byte tmp[] = new byte[op.getByteSize()];
            op.getBytes(tmp, 0);
            imp.execute(tmp, txnid);
        }
    }

    public void transactionAborted(ProxyTxnId txnid) {
        ProxyTxnId id = idmapping.get(txnid);
        txnrecord rec = records.get(id);
        rec.reset();
        idmapping.remove(txnid);
        txnid = imp.beginTxn();
        idmapping.put(txnid, id);
    }

    public void transactionCommitted(ProxyTxnId txnid) {
        ProxyTxnId id = idmapping.get(txnid);
        txnrecord rec = records.get(id);
        rec.finish();
        TxnRepMessage msg1 = new TxnRepMessage(id, 0);
        replyToUser(rec.getUser(), msg1);
        idmapping.remove(txnid);
        records.remove(id);
    }

    public int selectStorageServer(txstore.util.Operation op) {
        return 0;
    }

    public int selectStorageServer(byte[] op) {
        return 0;
    }

    @Override
    public void handle(byte[] b) {
        // TODO Auto-generated method stub
        MessageBase msg = mf.fromBytes(b);
        Debug.println(msg);
        if (msg == null) {
            throw new RuntimeException("Should never receive a null message");
        }

        if (msg.getTag() == MessageTags.TXNREQ) {
            process((TxnReqMessage) msg);

        } else {
            throw new RuntimeException("invalid message tag: " + msg.getTag());
        }
    }

    private void process(TxnReqMessage msg) {
        // TODO Auto-generated method stub
        int color = msg.getColor();
        issueTxn(msg);
    }

    public void replyToUser(int userId, TxnRepMessage msg) {
        sendToUser(msg, userId);
    }

    public static void main(String arg[]) {
        if (arg.length != 6) {
            System.out.println("usage: FakedProxy dc_config.xml dc_user_config.xml dcId proxyId aSthdcount proxythdcount");
            System.exit(-1);
        }
        OpenLoopFakedAppServer olfap = new OpenLoopFakedAppServer(arg[1], Integer.parseInt(arg[2]), Integer.parseInt(arg[3]));

        OpenLoopProxy imp = new OpenLoopProxy(arg[0], Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), olfap);
        olfap.setProxy(imp);

        // set up the networking for outgoing messages
        NettyTCPSender sendNet = new NettyTCPSender();
        imp.setSender(sendNet);

        // set up the networking for incoming messages
        // first, create the pipe from the network to the proxy
        int proxythdcount = Integer.parseInt(arg[5]);
        ParallelPassThroughNetworkQueue ptnq = new ParallelPassThroughNetworkQueue(imp, proxythdcount);
        // then create the actual network
        
        NettyTCPReceiver rcv = new NettyTCPReceiver(
                imp.getMembership().getMe().getInetSocketAddress(), ptnq, 2);

        // set up an appServer


        // set up the networking for outgoing messages
        NettyTCPSender sendNet1 = new NettyTCPSender();
        olfap.setSender(sendNet1);

        // set up the networking for incoming messages
       
        // first, create the pipe from the network to the appServer
          int aSthdcount = Integer.parseInt(arg[4]);
        ParallelPassThroughNetworkQueue ptnq1 = new ParallelPassThroughNetworkQueue(olfap, aSthdcount);
        // then create the actual network
     
        NettyTCPReceiver rcv1 = new NettyTCPReceiver(olfap.getMembership().getMe().getInetSocketAddress(), ptnq1, 4);
    }
}

class txnrecord {

    long start;
    long end;
    OperationList ops[];
    int count = 0;
    ProxyTxnId usertxnid;

    public txnrecord(ProxyTxnId user, long start, OperationList lst[]) {
        this.start = start;

        ops = lst;
        this.usertxnid = user;
    }

    public txnrecord(ProxyTxnId user, long start, OperationList lst) {
        this(user, start, new OperationList[]{lst});
    }

    public void setEnd(long e) {
        end = e;
    }

    public long latency() {
        return end - start;
    }

    public String toString() {
        return " (" + start + "->" + end + ") " + latency();
    }

    // possibly add microbenchmark timing to opissued and nextop
    public void opIssued() {
        count++;
    }

    public int getUser() {
        return usertxnid.getProxyId();
    }

    public ProxyTxnId getUserTxnId() {
        return usertxnid;
    }

    public OperationList nextOp() {

        return count < ops.length ? ops[count] : null;
    }

    public void reset() {
        count = 0;
    }

    public void finish() {
        setEnd(System.nanoTime());
    }
}
