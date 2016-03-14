package txstore.scratchpad.kvs.util;
import util.Debug;

import java.io.*;
import java.util.*;

import com.sleepycat.bind.tuple.*;
import txstore.scratchpad.kvs.*;
import txstore.util.Operation;
import txstore.util.Result;

/**
 * Encodes a key-value store operation
 * @author nmp
 */
public abstract class KVResult
	extends Result
{
	private static final short KVOP_GET = 1;
	private static final short KVOP_PUT = 2;
	private static final short KVOP_GENERIC_BOOLEAN = 1000;

	/**
	 * Create object to represent a get result
	 */
//	public static KVGetResult createGetResult( String table, Object data, KVTablesConfig tables) {
//		return createGetResult( data, tables.getConfig(table).dataBind);
//	}
	public static KVGetResult createGetResult( Object data, TupleBinding dataBind) {
		TupleOutput out = new TupleOutput();
//		out.writeString(table);
		out.writeByte(KVOP_GET);
		dataBind.objectToEntry( data, out);
		return new KVGetResult( out.getBufferBytes(), out.getBufferLength(), data);
	}


	/**
	 * Create object to represent a put result
	 * @param table
	 * @param key
	 * @param data
	 * @param tables
	 * @return
	 */
	public static KVPutResult createPutResult( boolean ok) {
		TupleOutput out = new TupleOutput();
//		out.writeString(table);
		out.writeShort(KVOP_PUT);
		out.writeBoolean(ok);
		return new KVPutResult( out.getBufferBytes(), out.getBufferLength(), ok);
	}

	/**
	 * Create object to represent a result of a boolean
	 */
	public static KVBooleanResult createBooleanResult( boolean ok) {
		TupleOutput out = new TupleOutput();
//		out.writeString(table);
		out.writeShort(KVOP_GENERIC_BOOLEAN);
		out.writeBoolean(ok);
		return new KVBooleanResult( out.getBufferBytes(), out.getBufferLength(), ok);
	}

	public static KVResult extractResult( Result res, String tableName, KVTablesConfig tables) throws IOException {
		TupleInput in = new TupleInput( res.getResult(), 0, res.getByteSize());
//		String tableName = in.readString();
		KVTableConfig tableConfig = tables.getConfig(tableName);
		short type = in.readShort();
		Object key, data;
		boolean ok;
		switch( type) {
		case KVOP_GET:
			data = tableConfig.dataBind.entryToObject(in);
			return new KVGetResult( res, data);
		case KVOP_PUT:
			ok = in.readBoolean();
			return new KVPutResult( res, ok);
		case KVOP_GENERIC_BOOLEAN:
			ok = in.readBoolean();
			return new KVBooleanResult( res, ok);
		}
		throw new RuntimeException( "Not expected operation");
	}
	public KVResult( byte[] arr, int length) {
		super( arr);
	}
	public KVResult( Result result) {
		super( result.getResult());
	}
}
