package applications.microbenchmark.MemoryKVScratchpadTest;

import java.rmi.RemoteException;
import java.util.Vector;

import txstore.util.ProxyTxnId;
import txstore.scratchpad.ScratchpadInterface;
import util.Debug;

public class FakedMemKVDirectScratchpadFactory implements MemKVScratchpadFactory {

	int dcCount;

	public FakedMemKVDirectScratchpadFactory(int c, 
			int objectCount) {
		dcCount = c;
		FakedMemKVDirectScratchpad.setSharedVariables(c, objectCount);
	}

	public RemoteScratchpad createScratchPad() throws RemoteException {
		return new FakedMemKVDirectScratchpad(dcCount);
	}

}
