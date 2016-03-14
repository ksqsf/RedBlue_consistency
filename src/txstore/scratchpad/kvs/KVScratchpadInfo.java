package txstore.scratchpad.kvs;

import java.util.List;
import java.util.Set;

import txstore.scratchpad.kvs.util.KVOperation;
import txstore.scratchpad.kvs.util.KVOperationLog;
import txstore.util.ProxyTxnId;
import txstore.util.ReadSet;
import txstore.util.ReadSetEntry;
import txstore.util.WriteSet;
import txstore.util.WriteSetEntry;

import com.sleepycat.db.Transaction;

public class KVScratchpadInfo
{
	Transaction txn;
	ProxyTxnId curTxId;
	
	Set<WriteSetEntry> writeSet;
	Set<ReadSetEntry> readSet;
	WriteSet writeSetFinal;
	ReadSet readSetFinal;
	List<KVOperation> opLog;
	KVOperationLog opLogFinal;
	boolean remote;
	boolean readOnly;

	KVTablesConfig databases;
	
	public TableRuntimeInfo getRuntimeInfo( String table) {
		return (TableRuntimeInfo)databases.getConfig(table);
	}
}
