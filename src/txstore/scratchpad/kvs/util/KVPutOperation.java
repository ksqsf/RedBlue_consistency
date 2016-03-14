package txstore.scratchpad.kvs.util;

import txstore.util.Operation;

public class KVPutOperation
	extends KVOperation
{
	protected transient Object key;
	protected transient Object data;
	public KVPutOperation( byte[] arr, int length, String table, Object key, Object data) {
		super( arr, length, table);
		this.key = key;
		this.data = data;
	}
	public KVPutOperation(Operation op, String table, Object key, Object data) {
		super( op, table);
		this.key = key;
		this.data = data;
	}
	public Object getKey() {
		return key;
	}
	public Object getData() {
		return data;
	}
	public boolean isQuery() {
		return false;
	}

}
