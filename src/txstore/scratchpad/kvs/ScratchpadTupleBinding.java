package txstore.scratchpad.kvs;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class ScratchpadTupleBinding extends TupleBinding<ScratchpadTuple> {
	TupleBinding dataBind;
	
	public ScratchpadTupleBinding( TupleBinding bind) {
		this.dataBind = bind;
	}

    public void objectToEntry(ScratchpadTuple myData, TupleOutput to) {
        to.writeBoolean(myData.isDel());
        to.writeLong(myData.getTs());
        to.writeString(myData.getClock());
        dataBind.objectToEntry(myData.getObject(), to);
    }

    public ScratchpadTuple entryToObject(TupleInput ti) {
    	ScratchpadTuple myData = new ScratchpadTuple();
    	myData.setDel(ti.readBoolean());
    	myData.setTs(ti.readLong());
    	myData.setClock(ti.readString());
    	myData.setObject(dataBind.entryToObject(ti));
        return myData;
    }
} 
