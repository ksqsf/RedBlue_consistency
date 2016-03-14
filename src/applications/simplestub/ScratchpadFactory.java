package applications.simplestub;

import txstore.util.ProxyTxnId;
import txstore.scratchpad.ScratchpadInterface;

public class ScratchpadFactory implements txstore.scratchpad.ScratchpadFactory{

    int dcCount;
    int objectCount;
    int blue;

    public ScratchpadFactory(int c, int o, int b){
        dcCount = c; objectCount = o;blue = b;
        StubScratchPad.setSharedVariables(c, o);
    }

    public ScratchpadInterface createScratchPad(ProxyTxnId txn){
	ScratchpadInterface sp = new StubScratchPad(dcCount, objectCount);
	sp.beginTransaction(txn);
	return sp;
    }

    public void releaseScratchpad(ScratchpadInterface foo){}

	public int getAvailablePoolSize() {
		// TODO Auto-generated method stub
		return 0;
	}
}