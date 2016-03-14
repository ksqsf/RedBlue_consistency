package txstore.scratchpad.kvs.util;

import txstore.scratchpad.kvs.resolution.IKVStore;
import txstore.util.Operation;

public abstract class KVGenericOperation
	extends KVOperation
{
	protected transient Object key;
	protected KVGenericOperation( byte[] arr, int length, String table, Object key) {
		super( arr, length, table);
		this.key = key;
	}
	protected KVGenericOperation(Operation op, String table, Object key) {
		super( op, table);
		this.key = key;
	}
	public Object getKey() {
		return key;
	}
	public abstract boolean isQuery();
	/**
	 * Should return true if individual get/put operations must be registered for reexecution. 
	 * Otherwise, big operation is registered.
	 * @return
	 */
	public abstract boolean registerIndividualOperations();
	/**
	 * Execute the code of the operations
	 */
	public abstract KVResult execute( IKVStore store);
}
