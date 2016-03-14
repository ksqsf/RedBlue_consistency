package applications.simplestub;
import applications.versionzero.*;
import applications.simplestub.*;
import util.Debug;

import util.UnsignedTypes;

public class KVWrite extends Write {
    long val;
  
    
    public KVWrite(int i, int c, long val){
    	super(i,c);
    	this.val = val;
    
    }

    public KVWrite(byte[] b, int offset){
    	super( b, offset);
    offset += UnsignedTypes.intsize;
    offset += UnsignedTypes.intsize;
    val = UnsignedTypes.bytesToLong(b, offset);
    }

    public void getBytes(byte[] b, int offset){
	UnsignedTypes.intToBytes(id, b, offset);
    offset += UnsignedTypes.intsize;
    UnsignedTypes.intToBytes(color, b, offset);
    offset += UnsignedTypes.intsize;
    UnsignedTypes.longToBytes(val, b, offset);

    }

    public long getVal(){
	return val;
    }

    public static int getByteSize(){
	return UnsignedTypes.uint16Size*2 + UnsignedTypes.longsize;
    }

    public String toString(){
	return ""+getId()+":"+getColor()+":"+getVal();
    }
}
