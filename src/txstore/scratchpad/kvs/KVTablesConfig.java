package txstore.scratchpad.kvs;

import java.util.*;
import java.util.Map.Entry;

public class KVTablesConfig
{
	private Map<String,KVTableConfig> tables; 
	public KVTablesConfig() {
		tables = new HashMap<String,KVTableConfig>();
	}
	
	public KVTablesConfig( KVTablesConfig info) {
		tables = new HashMap<String,KVTableConfig>();
		Iterator<Entry<String,KVTableConfig>> it = info.tables.entrySet().iterator();
		while( it.hasNext()) {
			Entry<String,KVTableConfig> entry = it.next();
			TableRuntimeInfo c = (TableRuntimeInfo)entry.getValue();
			TableRuntimeInfo newC = new TableRuntimeInfo( c.db, c.policy.duplicate(), c.keyBind, c.dataBind, c.entryBind);
			tables.put(entry.getKey(), newC);
		}
	}

	public void addConfig( String table, KVTableConfig config) {
		tables.put(table, config);
	}
	
	public KVTableConfig getConfig( String table) {
		return tables.get( table);
	}
	
	public Iterator<Entry<String,KVTableConfig>> iterator() {
		return tables.entrySet().iterator();
	}
	
}
