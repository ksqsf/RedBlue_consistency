package txstore.scratchpad.kvs;

import util.Debug;
import com.sleepycat.bind.tuple.*;

public class KVTableConfig
{
	public TupleBinding keyBind;
	public TupleBinding dataBind;
	
	public KVTableConfig( TupleBinding keyBind, TupleBinding dataBind) {
		this.keyBind = keyBind;
		this.dataBind = dataBind;
	}
	
	public KVTableConfig( String keyBindClass, String dataBindClass) {
		try {
			this.keyBind = (TupleBinding)Class.forName(keyBindClass).newInstance();
			this.dataBind = (TupleBinding)Class.forName(dataBindClass).newInstance();
		} catch( Exception e) {
			Debug.kill(e);
		}
	}
	
}
