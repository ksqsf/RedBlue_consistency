package txstore.scratchpad.rdbms;
import util.Debug;

import java.util.*;
import txstore.scratchpad.*;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.scratchpad.resolution.ExecutionPolicy;
import txstore.util.*;

import java.sql.*;

import net.sf.jsqlparser.*;
import net.sf.jsqlparser.parser.*;

public class OCCDBScratchpad
	extends DBScratchpad
	implements ScratchpadInterface, IDBScratchpad
{
	
	public OCCDBScratchpad( ScratchpadConfig config) throws ScratchpadException {
		super( config);
	}
	
	@Override
	public Result execute(Operation op) throws ScratchpadException {
		DBOperation dbOp = null;
		if( op instanceof DBOperation)
			dbOp = (DBOperation)op;
		else if( op instanceof Operation)
			dbOp = new DBOperation( op);
		else
			throw new RuntimeException( "Expecting DBOperation, but object of class " + op.getClass().getName());
		if( dbOp.sql.charAt( dbOp.sql.length() - 1) == ';')
			dbOp.sql=dbOp.sql.substring(0,dbOp.sql.length() - 1);
		return super.execute( dbOp);
	}
			

}
