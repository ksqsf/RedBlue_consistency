package txstore.scratchpad.kvs;
import util.Debug;

import java.util.*;
import java.util.Map.Entry;
import java.io.*;

import txstore.scratchpad.*;
import com.sleepycat.bind.tuple.*;

import txstore.scratchpad.kvs.util.*;
import txstore.scratchpad.rdbms.util.*;
import txstore.scratchpad.resolution.*;
import txstore.util.*;

import java.sql.*;

import com.sleepycat.db.*;

import net.sf.jsqlparser.*;
import net.sf.jsqlparser.parser.*;

class KVBDBScratchpad
	implements IKVScratchpad
{
	protected Environment env;
	protected TransactionConfig txnConfig;
	protected KVTablesConfig databasesInt;

    protected KVScratchpadFactory myFactory;
	
    static KVBDBScratchpad instance;
    static KVBDBScratchpad getNewHandle( KVScratchpadConfig config, KVScratchpadInfo info)  throws ScratchpadException {
    	if( instance == null)
    		instance = new KVBDBScratchpad( config);
    	instance.initNewHandle( info);
    	return instance;
    }
    
	protected KVBDBScratchpad( KVScratchpadConfig config) throws ScratchpadException {
		Debug.println("Scratchpad init1\n");
		init( config.duplicate());
	}
	
	protected void initNewHandle( KVScratchpadInfo info) {
		info.writeSet = new HashSet<WriteSetEntry>();
		info.writeSetFinal = null;
		info.readSet = new HashSet<ReadSetEntry>();
		info.readSetFinal = null;
		info.opLog = new ArrayList<KVOperation>();
		
		info.databases = new KVTablesConfig( databasesInt);
	}
	
	protected void init( KVScratchpadConfig config) throws ScratchpadException {
		try {
			this.databasesInt = new KVTablesConfig();

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
			scratchpadDBInits( config);
		} catch( Exception e) {
			throw new ScratchpadException( e);
		}
	}	
	
	protected void scratchpadDBInits( KVScratchpadConfig config) throws ScratchpadException {
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
		    databasesInt.addConfig( tableName, new TableRuntimeInfo( db, pol, keyBinding, dataBinding, scratchpadBinding));

		}
		} catch( Exception e) {
			Debug.kill(e);
		}
	}
	

	public ReadSet getReadSet(KVScratchpadInfo info) {
		Debug.println( "READ SET = " + info.readSet);
		ReadSetEntry[] tmpList = new ReadSetEntry[info.readSet.size()];
		info.readSet.toArray(tmpList);
		for(int i = 0; i < tmpList.length; i++){
			ReadSetEntry e = tmpList[i];
			Debug.printf("read set entry %s \n", e.toString());
		}
		if( info.readSetFinal != null)
			return info.readSetFinal;
		info.readSetFinal = new ReadSet( info.readSet);
		return info.readSetFinal;
	}

	public WriteSet getWriteSet(KVScratchpadInfo info) {
		Debug.println( "WRITE SET = " + info.writeSet);
		if( info.writeSetFinal != null)
			return info.writeSetFinal;
		info.writeSetFinal = new WriteSet( info.writeSet);
		return info.writeSetFinal;
	}

	public void beginTransaction(KVScratchpadInfo info, ProxyTxnId txnId) {
		Debug.println("begin Txn " + txnId);
		info.writeSet.clear();
		info.readSet.clear();
		info.readSetFinal = null;
		info.writeSetFinal = null;
		info.opLog.clear();
		info.readOnly = true;
		info.remote = false;
		info.curTxId = txnId;

		Iterator<Entry<String,KVTableConfig>> it = info.databases.iterator();
		while( it.hasNext())
			((TableRuntimeInfo)it.next().getValue()).policy.beginTx();

	    try {
			info.txn = env.beginTransaction(null, txnConfig);
		} catch (DatabaseException e) {
			Debug.kill(e);
		}
	}

	public Result execute( KVScratchpadInfo info, Operation op) throws ScratchpadException {
		try {
			KVOperation kvOp = null;
			if( op instanceof KVOperation)
				kvOp = (KVOperation)op;
			else if( op instanceof Operation)
				kvOp = KVOperation.extractOperation(op, info.databases);
			else
				throw new RuntimeException( "Expecting KVOperation, but object of class " + op.getClass().getName());

			TableRuntimeInfo infoR = (TableRuntimeInfo)info.databases.getConfig( kvOp.getTargetTable());
			
			if( kvOp instanceof KVGetOperation) {
				KVGetOperation kvGetOp = (KVGetOperation)kvOp;
				return infoR.policy.executeTemporaryGet( info, kvGetOp.getKey(), infoR, this);
			}
			if( kvOp instanceof KVPutOperation) {
				KVPutOperation kvPutOp = (KVPutOperation)kvOp;
				addToOpLog(info,kvOp);
				return infoR.policy.executeTemporaryPut( info, kvPutOp.getKey(), kvPutOp.getData(),infoR, this);
			}
			if( kvOp instanceof KVGenericOperation) {
				KVGenericOperation kvGenOp = (KVGenericOperation)kvOp;
				if( ! kvGenOp.registerIndividualOperations())
					addToOpLog(info,kvOp);
				return infoR.policy.executeTemporaryGeneric( info, kvGenOp,infoR, this);
			}
			throw new ScratchpadException( "Unknow operation");
		} catch( Exception e) {
			throw new ScratchpadException( e);
		}
	}

	public ReadWriteSet complete(KVScratchpadInfo info) {
		return new ReadWriteSet( getReadSet( info), getWriteSet( info));
	}

	public OperationLog getOperationLog(KVScratchpadInfo info) throws ScratchpadException {
		Debug.println( "OP LOG = " + info.opLog);
		if( info.opLogFinal != null)
			return info.opLogFinal;
		info.opLogFinal = KVOperationLog.createLog( info.opLog, info.databases);
		return info.opLogFinal;
	}

	public void abort(KVScratchpadInfo info) throws ScratchpadException {
		try {
			info.txn.abort();
		} catch (DatabaseException e) {
			throw new ScratchpadException( e);
		}
	}

	public OperationLog commit(KVScratchpadInfo info,LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		applyOperationLog( info, info.opLog, lc, ts);
		return getOperationLog(info);
	}

	protected void applyOperationLog( KVScratchpadInfo info, List<KVOperation> opLog, LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		try {
			info.txn.abort();
			info.txn = env.beginTransaction(null, txnConfig);
						
			Iterator<KVOperation> it = opLog.iterator();
			while( it.hasNext()) {
				KVOperation op = it.next();
				String tableName = op.getTargetTable();
				TableRuntimeInfo infoR = (TableRuntimeInfo)info.databases.getConfig(tableName);
				if( infoR == null)
					throw new ScratchpadException( "No config for table " + tableName);
				else {
					if( op instanceof KVPutOperation) {
						KVPutOperation kvPutOp = (KVPutOperation)op;
						infoR.policy.executeDefinitePut( info, kvPutOp.getKey(), kvPutOp.getData(), lc, ts, infoR, this);
					} else if( op instanceof KVGenericOperation) {
						KVGenericOperation kvGenOp = (KVGenericOperation)op;
						infoR.policy.executeDefiniteGeneric( info, kvGenOp,lc, ts, infoR, this);
					} else  
						throw new ScratchpadException( "Not expected operation in applyLog : " + op.getClass());
				}
			}
			info.txn.commit();
		} catch( Exception e) {
			throw new ScratchpadException( e);
		}
	}
	
	
	public void applyOperationLog(KVScratchpadInfo info,OperationLog opLog, LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		if( opLog instanceof KVOperationLog) {
			applyOperationLog( info, ((KVOperationLog)opLog).getLog( info.databases) , lc, ts);
		} else {
			applyOperationLog( info, KVOperationLog.createLog( opLog), lc, ts);
		}
	}

	public void applyOperationLog(KVScratchpadInfo info, OperationLog opLog) throws ScratchpadException {
		if( opLog instanceof KVOperationLog) {
			info.opLogFinal = (KVOperationLog)opLog;
		} else {
			info.opLogFinal = KVOperationLog.createLog(opLog);
		}
		info.remote = true;
	}

	public void finalize(KVScratchpadInfo info, LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		if( info.remote)
			applyOperationLog( info, info.opLogFinal.getLog( info.databases), lc, ts);
		else
			applyOperationLog( info, info.opLog, lc, ts);
	}

	/**
	 * Executes a query in the scratchpad state.
	 */
	public Object executeGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR) throws DatabaseException {
		DatabaseEntry keyE = new DatabaseEntry();
		infoR.keyBind.objectToEntry(key, keyE);
		
		DatabaseEntry dataE = new DatabaseEntry();
		OperationStatus status;
		status = infoR.db.get(info.txn, keyE, dataE, null);
		if( status.equals( OperationStatus.NOTFOUND))
			return null;
		return infoR.entryBind.entryToObject( dataE);
	}

	/**
	 * Executes a put in the scratchpad state.
	 */
	public boolean executePut( KVScratchpadInfo info, Object key, Object data, TableRuntimeInfo infoR) throws DatabaseException {
		DatabaseEntry keyE = new DatabaseEntry();
		infoR.keyBind.objectToEntry(key, keyE);
		
		Debug.println("data type:" + data.getClass().getName() + " ; dada data type:" + ((ScratchpadTuple)data).getObject().getClass().getName() );
		
		DatabaseEntry dataE = new DatabaseEntry();
		infoR.entryBind.objectToEntry(data, dataE);

		OperationStatus status;
		status = infoR.db.put(info.txn, keyE, dataE);
		return status.equals( OperationStatus.SUCCESS);
	}
	
	public boolean addToWriteSet(KVScratchpadInfo info,KVWriteSetEntry entry) {
		Debug.println( "add to write set:" + entry);
		info.writeSetFinal = null;
		info.readOnly = false;
		return info.writeSet.add(entry);
	}

	public boolean addToReadSet(KVScratchpadInfo info,KVReadSetEntry entry) {
		Debug.println( "add to read set:" + entry);
		info.readSetFinal = null;
		return info.readSet.add(entry);
	}

	public void addToOpLog(KVScratchpadInfo info,KVOperation op) {
		Debug.println( "add to op log: (" + op + ")");
		info.opLogFinal = null;
		info.opLog.add(op);
	}

	public boolean isReadOnly(KVScratchpadInfo info) {
		return info.readOnly;
	}


}

