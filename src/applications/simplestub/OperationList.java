package applications.simplestub;
import util.Debug;

import util.UnsignedTypes;

import java.util.Vector;

public class OperationList implements java.io.Serializable {

	Vector<Read> reads;
	Vector<Write> writes;

	public OperationList() {
		reads = new Vector<Read>();
		writes = new Vector<Write>();
	}

	public OperationList(byte[] b, int offset) {
		this();
		int sz = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
//                System.out.println("number of reads: "+sz);
		for (int i = 0; i < sz; i++) {
			addRead(new Read(b, offset));
			offset += Read.getByteSize();
		}
		sz = UnsignedTypes.bytesToInt(b, offset);
		offset += UnsignedTypes.uint16Size;
  //              System.out.println("number of writes: "+sz);
		for (int i = 0; i < sz; i++) {
			addWrite(new Write(b, offset));
			offset += Write.getByteSize();
		}
	}

	public void getBytes(byte[] b, int offset) {
		UnsignedTypes.intToBytes(reads.size(), b, offset);
		offset += UnsignedTypes.uint16Size;
    //            System.out.println(reads.size());
		for (int i = 0; i < reads.size(); i++) {
			reads.elementAt(i).getBytes( b, offset);
			offset += Read.getByteSize();
		}
		UnsignedTypes.intToBytes(writes.size(), b, offset);
		offset += UnsignedTypes.uint16Size;
		for (int i = 0; i < writes.size(); i++) {
			writes.elementAt(i).getBytes(b, offset);
			offset += Write.getByteSize();
		}
	}

	public Vector<Read> getReads() {
		return reads;
	}

	public Vector<Write> getWrites() {
		return writes;
	}

	public void addRead(Read rd) {
		reads.add(rd);
	}

	public void addWrite(Write w) {
		writes.add(w);
	}
	
	public int getColor(){
		for (int i = 0; i < writes.size(); i++) {
			if (writes.elementAt(i).color == 1){
				return 1;
			}
		}
		return 0;
	}

	public int getByteSize() {
		return computeByteSize(reads, writes);
	}

	public static int computeByteSize(Vector<Read> rd, Vector<Write> wr) {
		return (rd.size()*Read.getByteSize() + wr.size()*Write.getByteSize())+2 * UnsignedTypes.uint16Size;
	}

        
        public static void main(String arg[]){
            
            int i = 45;
            double blueRate = 0.5;
            OperationList opList = new OperationList();
            for (int k = 0; k < 4; k++) {
                int id = (int) ((i * k + i + i / (k + 1))) % 100;
                if (id < 0) {
                    id = -id;
                }
                int color = Math.random() < blueRate ? 1 : 0;
                System.out.println("writing; " + id + ":" + color);

                opList.addWrite(new Write(id, color));
                id = (int) ((i * i + k * k + i / (k + 1))) % 100;
                if (id < 0) {
                    id = -id;
                }
                color = Math.random() < blueRate ? 1 : 0;
                System.out.println("reading: " + id + ":" + color);
                opList.addRead(new Read(id, color));
            }
            System.out.println(opList.getWrites());
            System.out.println(opList.getReads());
            byte[] tmp = new byte[opList.getByteSize()];
            opList.getBytes(tmp, 0);

            OperationList ol2 = new OperationList(tmp, 0);
            System.out.println(ol2.getWrites());

        }
        
}