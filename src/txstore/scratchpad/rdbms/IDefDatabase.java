package txstore.scratchpad.rdbms;

import java.sql.ResultSet;
import java.sql.SQLException;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBSingleOperation;
import txstore.scratchpad.rdbms.util.DBUpdateResult;

public interface IDefDatabase
{
	public ResultSet executeQuery( String sql) throws SQLException;
	public int executeUpdate( String sql) throws SQLException, ScratchpadException;
	public int executeOp( String sql) throws SQLException, ScratchpadException;
	public void addCleanUpToBatch(String sql) throws SQLException;

}
