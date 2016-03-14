package txstore.scratchpad.kvs.tests;

import txstore.scratchpad.kvs.resolution.IKVStore;
import txstore.scratchpad.kvs.tests.data.MicroTuple;
import txstore.scratchpad.kvs.util.*;
import txstore.util.Operation;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.db.DatabaseException;

/**
 * In MicroTuple, set "e" to lower case if b >= param1 and to uppercase if b < param1 is even.
 */
public class ChangeStrInMicroTuple
	extends KVGenericOperation
{
	public static ChangeStrInMicroTuple createOperation( String table, Object key, long param1, TupleBinding keyBind) {
		TupleOutput out = new TupleOutput();
		out.writeString(table);
		out.writeShort(KVOperation.KVOP_CSMT);
		keyBind.objectToEntry( key, out);
		out.writeLong(param1);
		return new ChangeStrInMicroTuple( out.getBufferBytes(), out.getBufferLength(), table, key, param1);
	}
	
	protected long param1;

	protected ChangeStrInMicroTuple( byte[] arr, int len, String table, Object key, long param1) {
		super( arr, len, table, key);
		this.param1 = param1;
	}

	public ChangeStrInMicroTuple(Operation op, String tableName, Object key, long param1) {
		super( op, tableName, key);
		this.param1 = param1;
	}

	public long getParam1() {
		return param1;
	}
	public boolean isQuery() {
		return false;
	}
	public boolean registerIndividualOperations() {
		return false;
	}

	@Override
	public KVResult execute(IKVStore store) {
		try {
			MicroTuple tuple = (MicroTuple)store.get( this.table, this.key);
			if( tuple.b >= param1)
				tuple.e = tuple.e.toLowerCase();
			else
				tuple.e = tuple.e.toUpperCase();
			boolean result = store.put(table, key, tuple);
			return KVResult.createBooleanResult(result);
		} catch( DatabaseException e) {
			return KVResult.createBooleanResult(false);
		}
	}



}
