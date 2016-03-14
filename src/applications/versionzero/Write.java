package applications.versionzero;

import util.Debug;

import util.UnsignedTypes;

public class Write{
    int id;
    int color;
    int op;
    
    public Write(int i, int c, int o){
	id = i;
        color = c;
        op = o;
    }

    public Write(byte[] b, int offset){
	id = UnsignedTypes.bytesToInt(b, offset);
        offset += UnsignedTypes.uint16Size;
        color = UnsignedTypes.bytesToInt(b, offset);
        offset += UnsignedTypes.uint16Size;
        op = UnsignedTypes.bytesToInt(b, offset);
    }

    public void getBytes(byte[] b, int offset){
	UnsignedTypes.intToBytes(id, b, offset);
        offset += UnsignedTypes.uint16Size;
        UnsignedTypes.intToBytes(color, b, offset);
        offset += UnsignedTypes.uint16Size;
        UnsignedTypes.intToBytes(op, b, offset);

    }

    public int getId(){
	return id;
    }

    public int getColor(){
        return color;    
    }
    
    public int getOp(){
        return op;
    }
    
    static public int getByteSize(){
	return UnsignedTypes.uint16Size*3;
    }

    public String toString(){
	return ""+getId()+":"+getColor()+":"+getOp();
    }
}