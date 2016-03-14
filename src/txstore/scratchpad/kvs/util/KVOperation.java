package txstore.scratchpad.kvs.util;
import util.Debug;

import java.io.*;
import java.util.*;

import com.sleepycat.bind.tuple.*;
import txstore.scratchpad.kvs.*;
import txstore.scratchpad.kvs.tests.ChangeStrInMicroTuple;
import txstore.util.Operation;

/**
 * Encodes a key-value store operation
 * @author nmp
 */
public abstract class KVOperation
	extends Operation
{
	public static final short KVOP_GET = 1;
	public static final short KVOP_PUT = 2;
	public static final short KVOP_CSMT = 1000;
	/**
	 * Create object to represent a get operation
	 * @param table
	 * @param key
	 * @param tables
	 * @return
	 */
	public static KVGetOperation createGetOperation( String table, Object key, KVTablesConfig tables) {
		return createGetOperation( table, key, tables.getConfig(table).keyBind);
	}
	public static KVGetOperation createGetOperation( String table, Object key, TupleBinding keyBind) {
		TupleOutput out = new TupleOutput();
		out.writeString(table);
		out.writeShort(KVOP_GET);
		keyBind.objectToEntry( key, out);
		return new KVGetOperation( out.getBufferBytes(), out.getBufferLength(), table, key);
	}

	/**
	 * Create object to represent a put operation
	 * @param table
	 * @param key
	 * @param data
	 * @param tables
	 * @return
	 */
	public static KVPutOperation createGetOperation( String table, Object key, Object data, KVTablesConfig tables) {
		KVTableConfig config = tables.getConfig(table);
		return createPutOperation( table, key, data, config.keyBind, config.dataBind);
	}
	public static KVPutOperation createPutOperation( String table, Object key, Object data, TupleBinding keyBind, TupleBinding dataBind) {
		TupleOutput out = new TupleOutput();
		out.writeString(table);
		out.writeShort(KVOP_PUT);
		keyBind.objectToEntry( key, out);
		dataBind.objectToEntry( data, out);
		return new KVPutOperation( out.getBufferBytes(), out.getBufferLength(), table, key, data);
	}

	public static void dumpOneOperation( KVOperation op, TupleOutput out, KVTablesConfig tables) throws IOException {
		String tableName = op.getTargetTable(); 
		out.writeString( tableName);
		KVTableConfig tableConfig = tables.getConfig(tableName);
		if( op instanceof KVGetOperation) {
			out.writeShort(KVOP_GET);
			tableConfig.keyBind.objectToEntry( ((KVGetOperation)op).key, out);
		} else if( op instanceof KVPutOperation) {
			KVPutOperation opP = (KVPutOperation)op;
			out.writeShort(KVOP_PUT);
			tableConfig.keyBind.objectToEntry( opP.key, out);
			tableConfig.dataBind.objectToEntry( opP.data, out);
		} else if( op instanceof ChangeStrInMicroTuple) {
			ChangeStrInMicroTuple opP = (ChangeStrInMicroTuple)op;
			out.writeShort(KVOP_CSMT);
			tableConfig.keyBind.objectToEntry( opP.key, out);
			out.writeLong(opP.getParam1());
		} else
			throw new RuntimeException( "Not expect operation");
	}
	public static KVOperation extractOneOperation( TupleInput in, KVTablesConfig tables) throws IOException {
		return extractOneOperation( null, in, tables);
	}
	public static KVOperation extractOperation( Operation op, KVTablesConfig tables) throws IOException {
		TupleInput in = new TupleInput( op.getOperation(), 0, op.getByteSize());
		return extractOneOperation( op, in, tables);
	}

		protected static KVOperation extractOneOperation( Operation op, TupleInput in, KVTablesConfig tables) throws IOException {
		String tableName = in.readString();
		KVTableConfig tableConfig = tables.getConfig(tableName);
		short type = in.readShort();
		Object key, data;
		switch( type) {
		case KVOP_GET:
			key = tableConfig.keyBind.entryToObject(in);
			return new KVGetOperation( op, tableName, key);
		case KVOP_PUT:
			key = tableConfig.keyBind.entryToObject(in);
			data = tableConfig.dataBind.entryToObject(in);
			return new KVPutOperation( op, tableName, key, data);
		case KVOP_CSMT:
			key = tableConfig.keyBind.entryToObject(in);
			long param1 = in.readLong();
			return new ChangeStrInMicroTuple( op, tableName, key, param1);
		}
		throw new RuntimeException( "Not expected operation");
	}

	protected transient String table;
	
	protected KVOperation( byte[] arr, int length, String table) {
		super( arr);
		this.table = table; 
	}
	protected KVOperation( Operation op, String table) {
		super( op.getOperation());
		this.table = table; 
	}
	public abstract boolean isQuery();

	public String getTargetTable() {
		return table;
	}
}
