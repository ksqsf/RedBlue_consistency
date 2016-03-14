package txstore.scratchpad.kvs;
import util.Debug;

import java.util.*;
import java.util.Map.Entry;

import txstore.scratchpad.resolution.*;

public class KVScratchpadConfig
{
	protected String directory;
	protected String padClass;
	protected Map<String,KVExecutionPolicy> policies;
	protected Map<String,String> keyBindingClass;
	protected Map<String,String> dataBindingClass;
	
	public KVScratchpadConfig( String directory, String padClass) {
		this.directory = directory;
		this.padClass = padClass;
		this.policies = new HashMap<String,KVExecutionPolicy>();
		this.keyBindingClass = new HashMap<String,String>();
		this.dataBindingClass = new HashMap<String,String>();
	}
	protected KVScratchpadConfig( String directory, String padClass, Map<String,KVExecutionPolicy> p, 
			Map<String,String> kb, Map<String,String> db) {
		this.directory = directory;
		this.padClass = padClass;
		this.policies = new HashMap<String,KVExecutionPolicy>( p);
		this.keyBindingClass = new HashMap<String,String>( kb);
		this.dataBindingClass = new HashMap<String,String>( db);
	}
	public KVScratchpadConfig duplicate() {
		return new KVScratchpadConfig( directory, padClass, policies, keyBindingClass, dataBindingClass);
	}
	public String getPadClass()  {
		return padClass;
	}
	public String getDirectory()  {
		return directory;
	}
	public Collection<KVExecutionPolicy> getPolicies() {
		return policies.values();
	}
	public KVExecutionPolicy getPolicy( String tableName) {
		return policies.get(tableName);
	}
	public String getKeyBindingPolicy( String tableName) {
		return keyBindingClass.get(tableName);
	}
	public String getDataBindingPolicy( String tableName) {
		return dataBindingClass.get(tableName);
	}
	public void putTableInfo( String tableName, KVExecutionPolicy policy, String keyBinding, String dataBinding) {
		policies.put(tableName, policy);
		keyBindingClass.put(tableName, keyBinding);
		dataBindingClass.put(tableName, dataBinding);
	}
}
