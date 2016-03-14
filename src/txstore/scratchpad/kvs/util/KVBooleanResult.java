package txstore.scratchpad.kvs.util;

import txstore.util.*;

public class KVBooleanResult
	extends KVResult
{
	protected transient boolean ok;
	public KVBooleanResult( byte[] arr, int length, boolean ok) {
		super( arr, length);
		this.ok = ok;
	}
	public KVBooleanResult(Result res, boolean ok) {
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
