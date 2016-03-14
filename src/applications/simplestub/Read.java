package applications.simplestub;
import util.Debug;

import util.UnsignedTypes;

public class Read implements java.io.Serializable {
    int id;
    int color;
    
    public Read(int i, int c){
	id = i;
        color = c;
    }

    public Read(byte[] b, int offset){
	id = UnsignedTypes.bytesToInt(b, offset);
         offset += UnsignedTypes.intsize;
        color = UnsignedTypes.bytesToInt(b, offset);
    }

    public void getBytes(byte[] b, int offset){
	UnsignedTypes.intToBytes(id, b, offset);
        offset += UnsignedTypes.intsize;
        UnsignedTypes.intToBytes(color, b, offset);

    }
    
    public int getId(){
	return id;
    }

    
    public int getColor(){
        
        return color;
    }

    public static int getByteSize(){
	return UnsignedTypes.uint16Size*2;
    }

    public String toString(){
	return "" + getId()+":"+getColor();
    }
}