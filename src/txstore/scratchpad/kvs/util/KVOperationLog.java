package txstore.scratchpad.kvs.util;
import util.Debug;

import java.util.*;

import java.io.*;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.util.FastOutputStream;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.kvs.KVTablesConfig;
import txstore.scratchpad.rdbms.util.DBOperationLog;
import txstore.util.OperationLog;
import txstore.util.Result;

public class KVOperationLog
	extends OperationLog
{
	transient boolean hasDecoded;
	transient List<KVOperation> log;
	
	public static KVOperationLog createLog( List<KVOperation> log, KVTablesConfig tables) throws ScratchpadException {
		try {
			TupleOutput tout = new TupleOutput();
			tout.writeShort( log.size());
			Iterator<KVOperation> it = log.iterator();
			while( it.hasNext()) {
				KVOperation op = it.next();
				KVOperation.dumpOneOperation(op, tout, tables);
			}
			return new KVOperationLog( log, tout.toByteArray());
		} catch (IOException e) {
			throw new ScratchpadException( "Cannot encode result", e);
		}
	}
	
	public static KVOperationLog createLog( OperationLog log) throws ScratchpadException {
		return new KVOperationLog( log.getOperationLogBytes());
	}
	protected KVOperationLog( List<KVOperation> log, byte[] logBA) {
		super( logBA);
		this.log = log;
		this.hasDecoded = true;
	}
	
	protected KVOperationLog( byte[] logBA) {
		super( logBA);
		this.log = null;
		this.hasDecoded = false;
	}
	
	private void decode( KVTablesConfig tables) throws ScratchpadException {
		try {
			hasDecoded = true;
			TupleInput tin = new TupleInput( super.getOperationLogBytes());
			ByteArrayInputStream bais = new ByteArrayInputStream( super.getOperationLogBytes());
			DataInputStream dis = new DataInputStream( bais);
			log = new ArrayList<KVOperation>();
			int nLines = dis.readShort();
			if( nLines == 0)
				return;
			for( int i = 0; i < nLines; i++) {
				KVOperation op = KVOperation.extractOneOperation(tin, tables);
				log.add(op);
			}
		} catch( IOException e) {
			throw new ScratchpadException( "Cannot decode result", e);
		}
	}
		
	public List<KVOperation> getLog(KVTablesConfig tables) throws ScratchpadException {
		if( ! hasDecoded)
			decode( tables);
		return log;
	}

	
/*	public String toString() {
		if( ! hasDecoded)
			try {
				decode();
			} catch (ScratchpadException e) {
				// do ntohing
			}
		StringBuffer buffer = new StringBuffer();
		for( int i = 0; i < log.size(); i++) {
			DBOpPair op = log.get(i);
			buffer.append( "(");
			buffer.append( op.op.sql);
			buffer.append( ",");
			buffer.append(op.pk);
			buffer.append( ")");
			buffer.append( "\n");
		}
		return buffer.toString();
	}
*/
}
