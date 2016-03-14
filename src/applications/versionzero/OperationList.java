package applications.versionzero;

import util.Debug;

import util.UnsignedTypes;

import java.util.Vector;

public class OperationList {

	Vector<Write> writes;

	public OperationList() {
		writes = new Vector<Write>();
	}

	public OperationList(byte[] b, int offset) {
		this();
		
		int sz = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
		for (int i = 0; i < sz; i++) {
			addWrite(new Write(b, offset));
			offset += Write.getByteSize();
		}
	}

	public void getBytes(byte[] b, int offset) {
		
		UnsignedTypes.intToBytes(writes.size(), b, offset);
		offset += UnsignedTypes.uint16Size;
		for (int i = 0; i < writes.size(); i++) {
                    writes.elementAt(i).getBytes(b, offset);
                    offset+= Write.getByteSize();
		}
	}

	

	public Vector<Write> getWrites() {
		return writes;
	}

        
        public int getSize(){
            return writes.size();        
        }
        
        public Write getWrite(int index){
            return writes.elementAt(index);        
        }

	final public void addWrite(Write w) {
		writes.add(w);
	}

	public int getByteSize() {
		return computeByteSize(writes);
	}

	public static int computeByteSize( Vector<Write> wr) {
		return ( wr.size() * Write.getByteSize()) + UnsignedTypes.uint16Size;
	}

}