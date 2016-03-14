package txstore.scratchpad.kvs.tests;

import txstore.scratchpad.kvs.util.KVResult;
import txstore.util.*;

public class CSMTResult
	extends KVResult
{
	protected transient boolean ok;
	public CSMTResult( byte[] arr, int length, boolean ok) {
		super( arr, length);
		this.ok = ok;
	}
	public CSMTResult(Result res, boolean ok) {
		super( res);
		this.ok = ok;
	}
	public boolean success() {
		return ok;
	}
	public String toString() {
		return "" + ok;
	}
}
