package txstore.scratchpad.kvs.util;

import txstore.util.*;

public class KVGetResult
	extends KVResult
{
	protected transient Object data;
	public KVGetResult( byte[] arr, int length, Object data) {
		super( arr, length);
		this.data = data;
	}
	public KVGetResult(Result res, Object data) {
		super( res);
		this.data = data;
	}
	public Object getData() {
		return data;
	}
	public String toString() {
		return "" + data;
	}

}
