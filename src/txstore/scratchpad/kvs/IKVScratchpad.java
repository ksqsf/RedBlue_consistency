package txstore.scratchpad.kvs;
import util.Debug;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.sleepycat.db.DatabaseException;

import txstore.scratchpad.kvs.util.KVOperation;
import txstore.scratchpad.kvs.util.KVReadSetEntry;
import txstore.scratchpad.kvs.util.KVWriteSetEntry;
import txstore.scratchpad.rdbms.util.DBOpPair;
import txstore.scratchpad.rdbms.util.DBReadSetEntry;
import txstore.scratchpad.rdbms.util.DBWriteSetEntry;
import txstore.util.ReadSetEntry;

public interface IKVScratchpad
{
	/**
	 * Returns true if current transaction is read-only.
	 */
	boolean isReadOnly(KVScratchpadInfo info);

	/**
	 * Executes a query in the scratchpad state.
	 */
	Object executeGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR) throws DatabaseException;
	/**
	 * Executes an update in the scratchpad state.
	 */
	boolean executePut( KVScratchpadInfo info, Object key, Object data, TableRuntimeInfo infoR) throws DatabaseException;

	
	/**
	 * Add an update to the batch in the scratchpad state.
	 */
//	void addToBatchUpdate( String op) throws SQLException;
	/**
	 * Execute operations in the batch so far
	 */
//	void executeBatch() throws SQLException;
	/**
	 * Add the given entry to the write set
	 */
	boolean addToWriteSet( KVScratchpadInfo info, KVWriteSetEntry entry);
	/**
	 * Add the given entry to the read set
	 */
	boolean addToReadSet( KVScratchpadInfo info, KVReadSetEntry readSetEntry);
	/**
	 * Add the given entry to the operation log
	 * 
	 */
	void addToOpLog( KVScratchpadInfo info, KVOperation op);
}
