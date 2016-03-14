package txstore.scratchpad.kvs.resolution;

import com.sleepycat.db.DatabaseException;

/**
 * Interface for key-value store
 * @author nmp
 *
 */
public interface IKVStore
{
	public Object get( String table, Object key) throws DatabaseException;
	public boolean put( String table, Object key, Object data) throws DatabaseException;
}
