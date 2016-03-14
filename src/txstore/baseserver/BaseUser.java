/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package txstore.baseserver;

import txstore.BaseNode;
import txstore.membership.Role;
import txstore.messages.MessageFactory;
import txstore.messages.MessageTags;
import txstore.messages.ResultMessage;
import txstore.messages.OperationMessage;
import txstore.messages.MessageBase;
import util.Debug;
import txstore.util.ProxyTxnId;
import txstore.util.Operation;


/**
 *
 * @author aclement
 * A user that interacts with the BaseServer.  Sends requests to the base 
 * server and waits for responses.  
 */
public abstract class BaseUser extends BaseNode{
    
    MessageFactory mf;
    UserApplication app;
    ProxyTxnId id;
    
    public BaseUser(String membershipFile, int myId,
                    UserApplication baseApp){
        super(membershipFile, 0, Role.PROXY, myId);
        mf = new MessageFactory();
        app = baseApp;
        id = new ProxyTxnId(0, myId, 0);
        
    }
    
    
    public void handle(byte[] b){
        MessageBase msg = mf.fromBytes(b);
	Debug.println(msg);
	if (msg == null)
		throw new RuntimeException("Should never receive a null message");
	switch (msg.getTag()) {
		case MessageTags.RESULT:
			process((ResultMessage) msg);
			return;
		default:
			throw new RuntimeException("invalid message tag: " + msg.getTag());
		}
    }
    
    protected void process(ResultMessage msg){
        app.processResult(msg.getResult());   
    }
    
    int count = 0;
    public void execute(Operation op ){
	int opId = count++;
	OperationMessage opMsg = new OperationMessage(id, op, opId);
        id = null;
        ProxyTxnId f = 
                new ProxyTxnId(id.getDatacenterId(), 
                id.getProxyId(), 
                id.getCount()+1);
        //	sendToStorage(opMsg, sId);
	sendToStorage(opMsg, 0);
    }
}