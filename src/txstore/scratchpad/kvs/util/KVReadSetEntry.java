package txstore.scratchpad.kvs.util;
import util.Debug;

import txstore.util.*;


public class KVReadSetEntry
	extends ReadSetEntry
{
	transient String table;
	transient Object key;
	transient boolean blue;

	public KVReadSetEntry(String table, Object key, boolean blue, LogicalClock l) {
    	super( table.hashCode() * 2011 + key.hashCode(), blue ? 1 : 0, l);
   		this.table = table;
		this.key = key;
		this.blue = blue;
	}
	
	public String toString() {
		return "(" + table + "," + key + "," + blue + "," + super.getLogicalClock() + ")";
	}

	public int hashCode()  {
		return (int) (super.getColor() * 2011 + super.getObjectId()) ;
	}
	
	public boolean equals( Object obj) {
		if( !( obj instanceof ReadSetEntry))
			return false;
		return super.getObjectId() == ((ReadSetEntry)obj).getObjectId() &&
					super.getColor() == ((ReadSetEntry)obj).getColor();
	}
	
}
