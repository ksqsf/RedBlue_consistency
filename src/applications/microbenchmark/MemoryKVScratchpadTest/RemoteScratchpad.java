package applications.microbenchmark.MemoryKVScratchpadTest;

import java.rmi.*;

import txstore.util.LogicalClock;
import txstore.util.Operation;
import txstore.util.OperationLog;
import txstore.util.Result;
import txstore.util.TimeStamp;

public interface RemoteScratchpad
	extends Remote
{
	void ping() throws RemoteException;
	void reset() throws RemoteException;
	void beginTransaction( MProxyTxnId id) throws RemoteException;
	Result execute( Operation op) throws RemoteException;
	void abort() throws RemoteException;
	boolean commit(LogicalClock lc, TimeStamp ts) throws RemoteException;
}
