package applications.microbenchmark.FakedScratchpadTest;

import java.util.Vector;

import txstore.util.ProxyTxnId;
import txstore.scratchpad.ScratchpadInterface;
import util.Debug;

public class FakedScratchpadFactory implements
		txstore.scratchpad.ScratchpadFactory {

	int dcCount;
	Vector<ScratchpadInterface> unusedSPs;
	int userNum;
	int scratchpadInusedNum;
	int maxSP = 0;
	int scratchpadInadvance = 0;

	public FakedScratchpadFactory(int c, 
			int uNum, int s, int objectCount) {
		dcCount = c;
		userNum = uNum;
		scratchpadInusedNum = 0;
		scratchpadInadvance = s;
		unusedSPs = new Vector<ScratchpadInterface>();
		FakedScratchpad.setSharedVariables(c, objectCount);
		initSPs();
	}

	public synchronized void initSPs() {
		ProxyTxnId pid = new ProxyTxnId();
		for (int i = 0; i < scratchpadInadvance; i++) {
			Debug.printf("initi create sp %d \n", i);
			unusedSPs.add(createNewSp());
		}
	}

	public ScratchpadInterface createScratchPad(ProxyTxnId txnId) {
		// TODO Auto-generated method stub
		ScratchpadInterface sp = getScratchpad();
		return sp;

	}

	public void releaseScratchpad(ScratchpadInterface sp) {
		synchronized (unusedSPs) {
			Debug.println("put one item back to unusedSPs");
			unusedSPs.add(sp);
			//System.out.printf("%d unused scratchpad\n", unusedSPs.size());
		}
		/*synchronized (synch) {
			scratchpadInusedNum--;
			System.out.printf("%d already used\n", scratchpadInusedNum);
		}*/
	}

	public ScratchpadInterface getScratchpad() {
		ScratchpadInterface sp = null;
		Debug.println("try to get a scratchpad");
		sp = getPadFromPool();
		if (sp == null) {
			sp = createNewSp();
		}
		/*synchronized (synch) {
			scratchpadInusedNum++;
			// Debug.printf("already created %d\n", scratchpadInusedNum);
			System.out.printf("%d already used\n", scratchpadInusedNum);
		}*/
		return sp;
	}

	public ScratchpadInterface getPadFromPool() {
		ScratchpadInterface sp = null;
		synchronized (unusedSPs) {
			Debug.printf("%d unused scratchpad\n", unusedSPs.size());
			if (!unusedSPs.isEmpty()) {
				// Debug.printf("get from pool, %d left\n", unusedSPs.size());
				sp = unusedSPs.remove(0);
				//System.out.printf("%d unused scratchpad\n", unusedSPs.size());
			}
		}
		return sp;
	}

	//Object synch = new Object();

	public ScratchpadInterface createNewSp() {

		ScratchpadInterface sp = new FakedScratchpad(dcCount);
		return sp;
	}

	public int getAvailablePoolSize() {
		// TODO Auto-generated method stub
		synchronized(unusedSPs){
			return unusedSPs.size();
		}
	}
	public void releaseScratchpad(ScratchpadInterface sp, ProxyTxnId txnId) {
        System.out.println("releaseScratchpad - TODO Auto-generated method stub");
        releaseScratchpad(sp);
	}
}
