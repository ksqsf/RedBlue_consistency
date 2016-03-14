package txstore.scratchpad.kvs.util;

import txstore.util.Operation;

public class KVGetOperation
	extends KVOperation
{
	protected transient Object key;
	public KVGetOperation( byte[] arr, int length, String table, Object key) {
		super( arr, length, table);
		this.key = key;
	}
	public KVGetOperation(Operation op, String table, Object key) {
		super( op, table);
		this.key = key;
	}
	public Object getKey() {
		return key;
	}
	public boolean isQuery() {
		return true;
	}
}
