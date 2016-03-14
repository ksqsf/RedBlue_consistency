package txstore.scratchpad.kvs.resolution;

import java.util.*;

import com.sleepycat.db.DatabaseException;

import txstore.scratchpad.kvs.IKVScratchpad;
import txstore.scratchpad.kvs.KVScratchpadInfo;
import txstore.scratchpad.kvs.ScratchpadTuple;
import txstore.scratchpad.kvs.TableRuntimeInfo;
import txstore.scratchpad.kvs.util.KVGenericOperation;
import txstore.scratchpad.kvs.util.KVGetResult;
import txstore.scratchpad.kvs.util.KVOperation;
import txstore.scratchpad.kvs.util.KVReadSetEntry;
import txstore.scratchpad.kvs.util.KVResult;
import txstore.scratchpad.kvs.util.KVWriteSetEntry;
import txstore.scratchpad.resolution.KVExecutionPolicy;
import txstore.util.LogicalClock;
import txstore.util.TimeStamp;

public class KVAbstractExecution
	implements KVExecutionPolicy
{
	private Map<Object,ScratchpadTuple> cache;
	private boolean inDefinite;
	private boolean blue;
	private String tableName;
	
	public KVAbstractExecution( String tableName, boolean blue) {
		this.tableName = tableName;
		this.blue = blue;
		this.cache = new TreeMap<Object,ScratchpadTuple>();
	}
	
	@Override
	public void beginTx() {
		cache.clear();
		inDefinite = false;
	}
	
	@Override
	public boolean isBlue() {
		return blue;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public KVExecutionPolicy duplicate() {
		return new KVAbstractExecution( tableName, blue);
	}

	private ScratchpadTuple doExecuteTemporaryGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		ScratchpadTuple data = cache.get(key);
		if( data != null)  {
			if( ! data.registered) {
				db.addToReadSet( info, new KVReadSetEntry( tableName, key, blue, data.lc));
				data.registered = true;
			}
			return data;
		}
		data = (ScratchpadTuple)db.executeGet(info, key, infoR);
		if( data == null)
			return null;
		if( ! data.registered) {
			db.addToReadSet( info, new KVReadSetEntry( tableName, key, blue, data.lc));
			data.registered = true;
		}
		cache.put(key, data);
		return data;
	}

	private ScratchpadTuple doExecuteDefiniteGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		if( ! inDefinite) {
			inDefinite = true;
			cache.clear();
		}
		ScratchpadTuple data = cache.get(key);
		if( data != null)  {
			return data;
		}
		data = (ScratchpadTuple)db.executeGet(info, key, infoR);
		if( data == null)
			return null;
		cache.put(key, data);
		return data;
	}

	@Override
	public KVResult executeTemporaryGet( KVScratchpadInfo info, Object key, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		ScratchpadTuple data = doExecuteTemporaryGet( info, key, infoR, db);
		if( data == null)
			return KVResult.createGetResult(null, infoR.dataBind);
		return KVResult.createGetResult(data.getObject(), infoR.dataBind);
	}
	private boolean doExecuteTemporaryPut(KVScratchpadInfo info,Object key, Object data0, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		ScratchpadTuple data = cache.get(key);
		if( data == null) {
			data = new ScratchpadTuple();
			cache.put(key, data);
		}
		data.setObject(data0);
		db.addToWriteSet( info, new KVWriteSetEntry( tableName, key, blue));
		return true;
	}
	@Override
	public KVResult executeTemporaryPut(KVScratchpadInfo info,Object key, Object data0, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		return KVResult.createPutResult( doExecuteTemporaryPut( info, key, data0, infoR, db));
	}
	private boolean doExecuteDefinitePut(KVScratchpadInfo info,Object key, Object data0, LogicalClock lc, TimeStamp ts, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		if( ! inDefinite) {
			inDefinite = true;
			cache.clear();
		}
		ScratchpadTuple data = cache.get(key);
		if( data == null) {
			data = new ScratchpadTuple();
			cache.put(key, data);
		}
		data.setObject(data0);
		data.setLc(lc);
		data.setTs(ts.toLong());
		return db.executePut(info, key, data, infoR);
	}
	@Override
	public boolean executeDefinitePut(KVScratchpadInfo info, Object key, Object data, LogicalClock lc, TimeStamp ts, TableRuntimeInfo infoR, IKVScratchpad db) throws DatabaseException{
		return doExecuteDefinitePut( info, key, data, lc, ts, infoR, db);
	}
	@Override
	public KVResult executeTemporaryGeneric(final KVScratchpadInfo info, final KVGenericOperation op, final TableRuntimeInfo infoR, final IKVScratchpad db) throws DatabaseException{
		return op.execute( new IKVStore() {

			@Override
			public Object get(String table, Object key) throws DatabaseException {
				TableRuntimeInfo localInfoR =  info.getRuntimeInfo(table);
				ScratchpadTuple data = ((KVAbstractExecution)localInfoR.policy).doExecuteTemporaryGet(info, key, localInfoR, db) ;
				if( data == null)
					return null;
				else return data.getObject();
			}

			@Override
			public boolean put(String table, Object key, Object data) throws DatabaseException {
				TableRuntimeInfo localInfoR =  info.getRuntimeInfo(table);
				return ((KVAbstractExecution)localInfoR.policy).doExecuteTemporaryPut(info, key, data, localInfoR, db) ;
			}
			
		});
	}
	@Override
	public KVResult executeDefiniteGeneric(final KVScratchpadInfo info, final KVGenericOperation op, final LogicalClock lc, final TimeStamp ts, final TableRuntimeInfo infoR, final IKVScratchpad db) throws DatabaseException{
		return op.execute( new IKVStore() {

			@Override
			public Object get(String table, Object key) throws DatabaseException {
				TableRuntimeInfo localInfoR =  info.getRuntimeInfo(table);
				ScratchpadTuple data = ((KVAbstractExecution)localInfoR.policy).doExecuteDefiniteGet(info, key, localInfoR, db) ;
				if( data == null)
					return null;
				else return data.getObject();
			}

			@Override
			public boolean put(String table, Object key, Object data) throws DatabaseException {
				TableRuntimeInfo localInfoR =  info.getRuntimeInfo(table);
				boolean result = ((KVAbstractExecution)localInfoR.policy).executeDefinitePut(info, key, data, lc, ts, localInfoR, db) ;
				return result;
			}
			
		});
	}

}

