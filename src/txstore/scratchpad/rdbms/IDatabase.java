package txstore.scratchpad.rdbms;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBSingleOperation;
import txstore.scratchpad.rdbms.util.DBUpdateResult;

public interface IDatabase
{
	public DBSelectResult executeQuery( String sql) throws ScratchpadException;
	public DBUpdateResult executeUpdate( String sql) throws ScratchpadException;

}
