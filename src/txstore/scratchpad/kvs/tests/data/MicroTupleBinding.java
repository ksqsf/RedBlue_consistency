package txstore.scratchpad.kvs.tests.data;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class MicroTupleBinding extends TupleBinding<MicroTuple>
{
    public void objectToEntry(MicroTuple myData, TupleOutput to) {
    	if( myData == null)
    		to.writeBoolean( true);
    	else {
    		to.writeBoolean( false);
    		to.writeLong(myData.a);
    		to.writeLong(myData.b);
    		to.writeInt(myData.c);
    		to.writeInt(myData.d);
    		to.writeString(myData.e);
    	}
    }

    public MicroTuple entryToObject(TupleInput ti) {
    	MicroTuple myData = new MicroTuple();
    	boolean isNull = ti.readBoolean();
    	if( isNull)
    		return null;
    	myData.a = ti.readLong();
    	myData.b = ti.readLong();
    	myData.c = ti.readInt();
    	myData.d = ti.readInt();
    	myData.e = ti.readString();
        return myData;
    }
}
