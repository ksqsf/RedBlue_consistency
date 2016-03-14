package txstore.scratchpad.kvs;
import util.Debug;

import java.util.*;
import java.io.*;

import txstore.scratchpad.*;
import com.sleepycat.bind.tuple.*;

import txstore.scratchpad.kvs.util.*;
import txstore.scratchpad.rdbms.util.*;
import txstore.scratchpad.resolution.*;
import txstore.util.*;

import java.sql.*;

import com.sleepycat.db.*;

import net.sf.jsqlparser.*;
import net.sf.jsqlparser.parser.*;

public class KVScratchpad
	implements ScratchpadInterface
{
	protected KVBDBScratchpad pad;
	protected KVScratchpadConfig config;
	protected KVScratchpadInfo info;
	
    protected KVScratchpadFactory myFactory;
	
	public KVScratchpad( KVScratchpadConfig config) throws ScratchpadException {
		Debug.println("Scratchpad init1\n");
		init( config.duplicate());
	}
	
    public void setFactory(KVScratchpadFactory fac){
	myFactory = fac;
    }
    
    public static void prepareDBScratchpad( KVScratchpadConfig config) throws ScratchpadException {
    	// do nothing
    }

	protected void init( KVScratchpadConfig config) throws ScratchpadException {
		info = new KVScratchpadInfo();
		pad = KVBDBScratchpad.getNewHandle(config, info);
	}	
	
	@Override
	public ReadSet getReadSet() {
		return pad.getReadSet(info);
	}

	@Override
	public WriteSet getWriteSet() {
		return pad.getWriteSet(info);
	}

	@Override
	public void beginTransaction(ProxyTxnId txnId) {
		pad.beginTransaction(info, txnId);
	}

	@Override
	public Result execute(Operation op) throws ScratchpadException {
		return pad.execute(info, op);
	}

	@Override
	public ReadWriteSet complete() {
		return pad.complete(info);
	}

	@Override
	public OperationLog getOperationLog() throws ScratchpadException {
		return pad.getOperationLog(info);
	}

	@Override
	public void abort() throws ScratchpadException {
		pad.abort(info);
	}

	@Override
	public OperationLog commit(LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		return pad.commit(info, lc, ts);
	}

	
/*	public void applyOperationLog(OperationLog opLog, LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		if( opLog instanceof KVOperationLog) {
			applyOperationLog( ((KVOperationLog)opLog).getLog( databases) , lc, ts);
		} else {
			applyOperationLog( KVOperationLog.createLog(opLog), lc, ts);
		}
	}
*/	
	@Override
	public void applyOperationLog(OperationLog opLog) throws ScratchpadException {
		pad.applyOperationLog( info, opLog);
	}

	@Override
	public void finalize(LogicalClock lc, TimeStamp ts) throws ScratchpadException {
		pad.finalize(info, lc, ts);
	}

}

