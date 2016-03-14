package applications.microbenchmark.MemoryKVScratchpadTest;

import java.rmi.RemoteException;
import java.util.Vector;

import txstore.util.ProxyTxnId;
import txstore.scratchpad.ScratchpadInterface;
import util.Debug;

public class FakedMemKVScratchpadFactory implements MemKVScratchpadFactory {

	int dcCount;

	public FakedMemKVScratchpadFactory(int c, 
			int objectCount) {
		dcCount = c;
		FakedMemKVScratchpad.setSharedVariables(c, objectCount);
	}

	public RemoteScratchpad createScratchPad()  throws RemoteException {
		return new FakedMemKVScratchpad(dcCount);
	}


}
