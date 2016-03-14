package applications.microbenchmark.MemoryKVScratchpadTest;

import java.rmi.RemoteException;

public interface MemKVScratchpadFactory
{
	public RemoteScratchpad createScratchPad() throws RemoteException ;
}
