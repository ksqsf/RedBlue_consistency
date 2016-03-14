package txstore.scratchpad.kvs;

import txstore.scratchpad.resolution.KVExecutionPolicy;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.db.Database;

public class TableRuntimeInfo extends KVTableConfig
{
	public Database db;
	public KVExecutionPolicy policy;
	public TupleBinding entryBind;	// include scratchpad info
	public TableRuntimeInfo(Database db, KVExecutionPolicy policy, TupleBinding keyBind, TupleBinding dataBind, TupleBinding entryBind) {
		super( keyBind, dataBind);
		this.db = db;
		this.policy = policy;
		this.entryBind = entryBind;
	}
}
