package txstore.scratchpad.kvs.util;
import util.Debug;

import txstore.util.WriteSetEntry;

public class KVWriteSetEntry
		extends WriteSetEntry
{
	transient String table;
	transient Object key;
	transient boolean blue;
	public KVWriteSetEntry(String table, Object key, boolean blue) {
    	super( table.hashCode() * 2011 + key.hashCode(), blue ? 1 : 0);
	    this.table = table;
		this.key = key;
		this.blue = blue;
	}
	
	public String toString() {
		return "(" + table + "," + key + "," + blue + ")";
	}

	public int hashCode()  {
		return (int) (super.getColor() * 2011 + super.getObjectId()) ;
	}
	
	public boolean equals( Object obj) {
		if( !( obj instanceof WriteSetEntry))
			return false;
		return super.getObjectId() == ((WriteSetEntry)obj).getObjectId() &&
					super.getColor() == ((WriteSetEntry)obj).getColor();
	}
	
}
