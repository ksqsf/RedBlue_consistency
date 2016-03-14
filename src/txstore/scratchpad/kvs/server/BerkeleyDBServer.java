package txstore.scratchpad.kvs.server;
import util.Debug;

import java.util.*;
import java.io.*;

import txstore.scratchpad.*;
import com.sleepycat.bind.tuple.*;

import txstore.scratchpad.kvs.IKVScratchpad;
import txstore.scratchpad.kvs.KVScratchpadConfig;
import txstore.scratchpad.kvs.KVTablesConfig;
import txstore.scratchpad.kvs.ScratchpadTupleBinding;
import txstore.scratchpad.kvs.TableRuntimeInfo;
import txstore.scratchpad.kvs.util.*;
import txstore.scratchpad.rdbms.util.*;
import txstore.scratchpad.resolution.*;
import txstore.util.*;
import com.sleepycat.db.*;

import java.rmi.server.*;
import java.rmi.*;
import java.sql.*;

public class BerkeleyDBServer
	extends UnicastRemoteObject
	implements IBerkeleyDBServer
{
	protected KVScratchpadConfig config;
	protected Environment env;
	protected TransactionConfig txnConfig;
	protected Map<Long,Transaction> txns;
	protected long nextTxn;
	
	protected KVTablesConfig databases;



	public BerkeleyDBServer( KVScratchpadConfig config) throws RemoteException, ScratchpadException {
		Debug.println("Scratchpad init1\n");
		init( config.duplicate());
	}
	
	protected void init( KVScratchpadConfig config) throws ScratchpadException {
		try {
			this.config = config;
			
			this.databases = new KVTablesConfig();
			this.txns = new TreeMap<Long,Transaction>();
			nextTxn = 0;
			
			new File( config.getDirectory()).mkdir();
			
		    EnvironmentConfig myEnvConfig = new EnvironmentConfig();
		    myEnvConfig.setInitializeCache(true);
		    myEnvConfig.setInitializeLocking(true);
		    myEnvConfig.setInitializeLogging(true);
		    myEnvConfig.setTransactional(true);
		    myEnvConfig.setMultiversion(true);
		    myEnvConfig.setAllowCreate(true);

		    env = new Environment(new File(config.getDirectory()), myEnvConfig);
			Debug.println("KVScratchpad init");
			
		    txnConfig = new TransactionConfig();
		    txnConfig.setSnapshot(true);
			
			Debug.println("Get an environment\n");
			scratchpadDBInits();
		} catch( Exception e) {
			throw new ScratchpadException( e);
		}
	}	
	
	protected void scratchpadDBInits() throws ScratchpadException {
		try {
	    DatabaseConfig dbConfig = new DatabaseConfig();
	    dbConfig.setTransactional(true);
	    dbConfig.setAllowCreate(true);
	    dbConfig.setType(DatabaseType.BTREE);

		Iterator<KVExecutionPolicy> it = config.getPolicies().iterator();
		while( it.hasNext()) {
			KVExecutionPolicy pol = it.next();
			String tableName = pol.getTableName();
			
		    Database db = env.openDatabase(null,               // txn handle
                    tableName,   // db file name
                    tableName,             // db name
                    dbConfig);
		    
		    TupleBinding keyBinding = (TupleBinding)Class.forName(config.getKeyBindingPolicy(tableName)).newInstance();
		    TupleBinding dataBinding = (TupleBinding)Class.forName(config.getDataBindingPolicy(tableName)).newInstance();
		    TupleBinding scratchpadBinding = new ScratchpadTupleBinding( dataBinding);
		    databases.addConfig( tableName, new TableRuntimeInfo( db, pol, keyBinding, dataBinding, scratchpadBinding));

		}
		} catch( Exception e) {
			Debug.kill(e);
		}
	}
	


	public long beginTransaction() throws RemoteException, DatabaseException {
		long txnId;
		synchronized( txns) {
			txnId = nextTxn++;
			txns.put(txnId, env.beginTransaction(null, txnConfig));
		}
		Debug.println("begin Txn " + txnId);
		return txnId;
	}


	public void abort( long id) throws RemoteException, DatabaseException {
		Transaction tx = null;
		synchronized( txns) {
			tx = txns.remove(id);
		}
		if( tx != null)
			tx.abort();
	}

	public OperationLog commit( long id) throws RemoteException, DatabaseException {
		Transaction tx = null;
		synchronized( txns) {
			tx = txns.remove(id);
		}
		if( tx != null)
			tx.abort();
	}

	/**
	 * Executes a query in the scratchpad state.
	 */
	public DatabaseEntry executeGet( long id, String table, DatabaseEntry key) throws RemoteException, DatabaseException {
		Transaction tx = null;
		synchronized( txns) {
			tx = txns.get(id);
		}
		if( tx != null) {
			
			tx.abort();
		} else
			throw new DatabaseException();

		
		DatabaseEntry keyE = new DatabaseEntry();
		info.keyBind.objectToEntry(key, keyE);
		
		DatabaseEntry dataE = new DatabaseEntry();
		OperationStatus status;
		status = info.db.get(txn, keyE, dataE, null);
		if( status.equals( OperationStatus.NOTFOUND))
			return null;
		return info.dataBind.entryToObject( dataE);
	}

	/**
	 * Executes a put in the scratchpad state.
	 */
	@Override
	public boolean executePut( Object key, Object data, TableRuntimeInfo info) throws DatabaseException {
		DatabaseEntry keyE = new DatabaseEntry();
		info.keyBind.objectToEntry(key, keyE);
		
		DatabaseEntry dataE = new DatabaseEntry();
		info.dataBind.objectToEntry(data, dataE);

		OperationStatus status;
		status = info.db.put(txn, keyE, dataE);
		return status.equals( OperationStatus.SUCCESS);
	}

	executebatch
	

}

