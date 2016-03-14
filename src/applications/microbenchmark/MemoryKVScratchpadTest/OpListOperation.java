package applications.microbenchmark.MemoryKVScratchpadTest;

import applications.simplestub.OperationList;
import txstore.util.Operation;

public class OpListOperation
		extends Operation
{
	OperationList list;
	static byte[] arr = new byte[10000];
	
	public OpListOperation(OperationList list) {
		super(null);
		this.list = list;
	}
	
/*	// dummt method - just to include time to do this
	public byte[] getOperation() {
		list.getBytes(arr, 0);
		return arr;
	}
*/
}
