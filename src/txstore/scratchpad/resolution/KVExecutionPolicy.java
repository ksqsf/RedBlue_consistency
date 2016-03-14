package txstore.scratchpad.resolution;
import util.Debug;

import java.sql.*;

import com.sleepycat.db.DatabaseException;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.kvs.IKVScratchpad;
import txstore.scratchpad.kvs.KVScratchpadInfo;
import txstore.scratchpad.kvs.TableRuntimeInfo;
import txstore.scratchpad.kvs.util.KVGenericOperation;
import txstore.scratchpad.kvs.util.KVGetOperation;
import txstore.scratchpad.kvs.util.KVResult;
import txstore.scratchpad.rdbms.*;
import txstore.scratchpad.rdbms.resolution.LWWLockExecution;
import txstore.scratchpad.rdbms.resolution.TableDefinition;
import txstore.scratchpad.rdbms.util.DBOpPair;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.util.LogicalClock;
import txstore.util.Result;
import txstore.util.TimeStamp;

/**
 * Interface for defining the execution and resolution policy for a given table
 * @author nmp
 */
public interface KVExecutionPolicy
{
	/**
	 * Called on begin transaction
	 */
	public void beginTx();
	/**
	 * Returns an unitialized fresh copy of this execution policy
	 */
	public KVExecutionPolicy duplicate();
	/**
	 * Returns true if it is a blue table
	 */
	public boolean isBlue();
	/**
	 * Returns the table name
	 */
	public String getTableName() ;
	/**
	 * Execute temporary get operation
	 */
	public KVResult executeTemporaryGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR, IKVScratchpad db)  throws DatabaseException;
	/**
	 * Execute temporary put operation
	 */
	public KVResult executeTemporaryPut(KVScratchpadInfo info, Object key, Object data, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException;
	/**
	 * Execute definite put operation
	 */
	public boolean executeDefinitePut(KVScratchpadInfo info,Object key, Object data, LogicalClock lc, TimeStamp ts, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException;
	/**
	 * Execute temporary generic operation
	 */
	public KVResult executeTemporaryGeneric(KVScratchpadInfo info, KVGenericOperation op, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException;
	/**
	 * Execute definite generic operation
	 */
	public KVResult executeDefiniteGeneric(KVScratchpadInfo info, KVGenericOperation op, LogicalClock lc, TimeStamp ts, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException;

	
	
	
	
	/**
	 * Returns the table definition for this execution policy
	 */
//	TableDefinition getTableDefinition() ;
	/**
	 * Returns the alias table name
	 */
//	String getAliasTable() ;
	/**
	 * Add deleted to where statement
	 */
//	void addDeletedKeysWhere( StringBuffer buffer) ;
	/**
	 * Returns what should be in the from clause in select statements
	 */
//	void addFromTable( StringBuffer buffer, boolean both, String[] tableNames) ;
	/**
	 * Returns the text for retrieving key and version vector in select statements
	 */
//	public void addKeyVVBothTable( StringBuffer buffer, String tableAlias);
	/**
	 * Called on scratchpad initialization for a given table.
	 * Allows to setup any internal state needed
	 */
//	void init( DatabaseMetaData dm, String tableName, int id, int tableId, IDBScratchpad db) throws ScratchpadException;

	/**
	 * Executes a query in the scratchpad temporary state.
	 * @throws ScratchpadException 
	 */
//	Result executeTemporaryQuery( DBOperation dbOp, IDBScratchpad db, String[] table) throws SQLException, ScratchpadException;

	/**
	 * Executes a query in the scratchpad temporary state for a query that combines multiple ExecutionPolicies.
	 * @throws ScratchpadException 
	 */
//	Result executeTemporaryQuery( DBOperation dbOp, IDBScratchpad db, ExecutionPolicy[] policies, String[][] table) throws SQLException, ScratchpadException;

	/**
	 * Executes an update in the scratchpad temporary state.
	 * @throws ScratchpadException 
	 */
//	Result executeTemporaryUpdate( DBOperation dbOp, IDBScratchpad db) throws SQLException, ScratchpadException;

	/**
	 * Executes an update in the scratchpad final state.
	 * @param b 
	 */
//	void executeDefiniteUpdate(DBOpPair dbOp, IDBScratchpad db, LogicalClock lc, TimeStamp ts, boolean b) throws SQLException;


}
