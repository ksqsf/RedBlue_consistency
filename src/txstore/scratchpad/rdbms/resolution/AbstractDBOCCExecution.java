package txstore.scratchpad.rdbms.resolution;
import util.Debug;

import java.sql.*;
import java.util.*;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.IDBScratchpad;
import txstore.scratchpad.rdbms.util.DBOpPair;
import txstore.scratchpad.rdbms.util.DBOperation;
import txstore.scratchpad.rdbms.util.DBReadSetEntry;
import txstore.scratchpad.rdbms.util.DBSelectResult;
import txstore.scratchpad.rdbms.util.DBSingleOpPair;
import txstore.scratchpad.rdbms.util.DBSingleOperation;
import txstore.scratchpad.rdbms.util.DBUpdateResult;
import txstore.scratchpad.rdbms.util.DBWriteSetEntry;
import txstore.scratchpad.resolution.ExecutionPolicy;
import txstore.util.LogicalClock;
import txstore.util.Result;
import txstore.util.TimeStamp;

public abstract class AbstractDBOCCExecution implements ExecutionPolicy
{
	TableDefinition def;
	int tableId;
	boolean blue;
	
	AbstractDBOCCExecution( boolean blue) {
		this.blue = blue;
	}
	/**
	 * Returns the table name
	 */
	public String getTableName() {
		return def.name;
	}
	
	@Override
	public void beginTx(IDBScratchpad db) {
		// do nothing
	}
	@Override
	public boolean isBlue() {
		return blue;
	}
	@Override
	public TableDefinition getTableDefinition() {
		return def;
	}
	@Override
	public String getAliasTable() {
		return def.getNameAlias();
	}
	@Override
	public void addDeletedKeysWhere(StringBuffer buffer) {
		// do nothing
	}
	@Override
	public void addFromTable(StringBuffer buffer, boolean both, String[] tableNames) {
		buffer.append( tableNames[0]);
		buffer.append( " as ");
		buffer.append( tableNames[1]);
	}
	@Override
	public void addKeyVVBothTable(StringBuffer buffer, String tableAlias) {
		String[] pks = def.getPksPlain();
		for( int i = 0; i < pks.length; i++) {
			buffer.append( tableAlias);
			buffer.append( ".");
			buffer.append( pks[i]);
			buffer.append( ",");
		}
		buffer.append( tableAlias);
		buffer.append( ".");
		buffer.append( DBScratchpad.SCRATCHPAD_COL_VV);
	}


	/**
	 * Process ResultSet and add it to the list of results.
	 * @throws SQLException 
	 */
	protected void addToResultList( ResultSet rs, List<String[]> result, IDBScratchpad db, int nResults) throws SQLException {
		if( rs == null)
			return;
		ResultSetMetaData rsmd = rs.getMetaData();
		int numberOfColumns = rsmd.getColumnCount();
		int nPks = def.getPksPlain().length;
		while( rs.next() && nResults > 0) {
			nResults--;
			String[] row = new String[numberOfColumns-1-nPks];
			for( int i = 0; i < row.length; i++)
				row[i] = rs.getString( i + 1);
			result.add(row);
			String[] pk = new String[nPks];
			for( int i = 0; i < nPks; i++)
				pk[i] = rs.getString( numberOfColumns - nPks + i);
			LogicalClock lc = new LogicalClock( rs.getString( numberOfColumns));
			db.addToReadSet( DBReadSetEntry.createEntry( def.name, pk, blue, lc));
		}
	}

	/**
	 * Process ResultSet and add it to the list of results.
	 * @throws SQLException 
	 */
	protected void addToResultList( ResultSet rs, List<String[]> result, IDBScratchpad db, ExecutionPolicy[] policies, int nResults) throws SQLException {
		if( rs == null)
			return;
		int numMetadata = policies.length;
		for( int i = 0; i < policies.length; i++) 
			numMetadata = numMetadata + policies[i].getTableDefinition().getPksPlain().length;
		ResultSetMetaData rsmd = rs.getMetaData();
		int numberOfColumns = rsmd.getColumnCount();
		while( rs.next() && nResults > 0) {
			nResults--;
			String[] row = new String[numberOfColumns-numMetadata];
			for( int i = 0; i < numberOfColumns-numMetadata; i++)
				row[i] = rs.getString( i + 1);
			result.add(row);
			for( int i = numberOfColumns-numMetadata, j = 0; i < policies.length; j++) {
				int nPks = policies[j].getTableDefinition().pkPlain.length;
				String[] pk = new String[nPks];
				for( int n = 0; n < pk.length; n++, i++)
					pk[n] = rs.getString( i+1);
				LogicalClock lc = new LogicalClock( rs.getString( ++i));
				db.addToReadSet( DBReadSetEntry.createEntry( policies[j].getTableDefinition().getName(), pk, policies[j].isBlue(), lc));
			}
		}
	}

	/**
	 * Add group by clause to the current buffer
	 */
	protected void addGroupBy( StringBuffer buffer, List l) {
		if( l == null || l.size() == 0)
			return;
		buffer.append( " order by ");
		Iterator it = l.iterator();
		while( it.hasNext()) {
			buffer.append( it.next());
			if( it.hasNext())
				buffer.append(",");
		}
	}

	/**
	 * Add group by clause to the current buffer
	 */
	protected void addGroupBy( StringBuffer buffer, List l, ExecutionPolicy[] policies, String[][] tables) {
		if( l == null || l.size() == 0)
			return;
		buffer.append( " group by ");
		Iterator it = l.iterator();
		while( it.hasNext()) {
			String s = it.next().toString();
			s = replaceAliasInStr(s, policies, tables);
			buffer.append( s);
			if( it.hasNext())
				buffer.append(",");
		}
	}

	/**
	 * Add order by clause to the current buffer
	 */
	protected void addOrderBy( StringBuffer buffer, List l) {
		if( l == null || l.size() == 0)
			return;
		buffer.append( " group by ");
		Iterator it = l.iterator();
		while( it.hasNext()) {
			buffer.append( it.next());
			if( it.hasNext())
				buffer.append(",");
		}
	}

	/**
	 * Add order by clause to the current buffer
	 */
	protected void addOrderBy( StringBuffer buffer, List l, ExecutionPolicy[] policies, String[][] tables) {
		if( l == null || l.size() == 0)
			return;
		buffer.append( " order by ");
		Iterator it = l.iterator();
		while( it.hasNext()) {
			String s = it.next().toString();
			s = replaceAliasInStr(s, policies, tables);
			buffer.append( s);
			if( it.hasNext())
				buffer.append(",");
		}
	}

	/**
	 * Add limit clause to the current buffer
	 * Return number in the limit clause
	 */
	protected int addLimit( StringBuffer buffer, Limit l) {
		if( l == null)
			return Integer.MAX_VALUE;
		//TODO: limit not supported in mimersql
		String s = l.toString().toLowerCase();
		s = s.substring( s.indexOf( "limit") + 5).trim();
		return Integer.parseInt(s);
	}

	/**
	 * Add where clause to the current buffer, removing needed deleted Pks
	 */
	protected void addWhere( StringBuffer buffer, Expression e) {
		if( e == null)
			return;
		buffer.append( " where ");
		buffer.append( DBScratchpad.SCRATCHPAD_COL_DELETED);
		buffer.append( " = FALSE");
		if( e != null) {
			buffer.append( " and ( ");
			buffer.append( e.toString());
			buffer.append( " ) ");
		}
	}
	
	/**
	 * Replace alias in string
	 * @param where
	 * @param policies
	 * @param tables
	 * @return
	 */
	protected String replaceAliasInStr( String where, ExecutionPolicy[] policies, String[][] tables) {
		return where;
	}

	/**
	 * Add where clause to the current buffer, removing needed deleted Pks
	 */
	protected void addWhere( StringBuffer buffer, Expression e, ExecutionPolicy[] policies, String[][] tables) {
		if( e == null)
			return;
		buffer.append( " where ");
		for( int i = 0; i < policies.length; i++) {
			if( i > 0)
				buffer.append( " and ");
			buffer.append( tables[i][1]);
			buffer.append(".");
			buffer.append( DBScratchpad.SCRATCHPAD_COL_DELETED);
			buffer.append( " = FALSE");
		}
		if( e != null) {
			buffer.append( " and ( ");
			String where = replaceAliasInStr( e.toString(), policies, tables);
			buffer.append( where);
			buffer.append( " ) ");
		}
		addDeletedKeysWhere(buffer);
	}

	@Override
	public void init(DatabaseMetaData dm, String tableName, int id, int tableId, IDBScratchpad db)
			throws ScratchpadException {
		try {
			this.tableId = tableId;
			String tempTableName = tableName + "_" + id;
			String tempTableNameAlias = DBScratchpad.SCRATCHPAD_TEMPTABLE_ALIAS_PREFIX + tableId;
			String tableNameAlias = DBScratchpad.SCRATCHPAD_TABLE_ALIAS_PREFIX + tableId;
			ArrayList<Boolean> tempIsStr = new ArrayList<Boolean>() ;		// for columns
			ArrayList<String> temp = new ArrayList<String>() ;
			ArrayList<String> tempAlias = new ArrayList<String>() ;	// for columns with aliases
			ArrayList<String> tempTempAlias = new ArrayList<String>() ;	// for temp columns with aliases
			ResultSet colSet = dm.getColumns(null, null, tableName, "%");
			boolean first = true;
			while (colSet.next()) {
				temp.add(colSet.getString( 4 ));
				tempAlias.add(  tableNameAlias + "." + colSet.getString( 4 ));
				tempTempAlias.add(  tempTableNameAlias + "." + colSet.getString( 4 ));
				tempIsStr.add(colSet.getInt(5) == java.sql.Types.VARCHAR || colSet.getInt(5) == java.sql.Types.LONGNVARCHAR || colSet.getInt(5) == java.sql.Types.LONGVARCHAR);
			}
			colSet.close();
			String[] cols = new String[temp.size()];
			temp.toArray(cols);
			temp.clear();

			String[] aliasCols = new String[tempAlias.size()];
			tempAlias.toArray(aliasCols);
			tempAlias.clear();

			String[] tempAliasCols = new String[tempTempAlias.size()];
			tempTempAlias.toArray(tempAliasCols);
			tempTempAlias.clear();

			boolean[] colsIsStr = new boolean[tempIsStr.size()];
			for( int i = 0; i < colsIsStr.length; i++)
				colsIsStr[i] = tempIsStr.get(i);

			ResultSet pkSet = dm.getPrimaryKeys(null, null, tableName);
			while (pkSet.next()) {
				temp.add(pkSet.getString( 4 ));
				tempAlias.add(  tableNameAlias + "." + pkSet.getString( 4 ));
				tempTempAlias.add(  tempTableNameAlias + "." + pkSet.getString( 4 ));
			}
			pkSet.close();
//			if( temp.size() != 1)
//				throw new RuntimeException( "Does not support table with more than one primary key column : " + tableName + ":" + temp);
			String[] pkPlain = new String[temp.size()];
			temp.toArray(pkPlain);
			temp.clear();

			String[] pkAlias = new String[tempAlias.size()];
			tempAlias.toArray(pkAlias);
			tempAlias.clear();

			String[] pkTempAlias = new String[tempTempAlias.size()];
			tempTempAlias.toArray(pkTempAlias);
			tempTempAlias.clear();
			
			def = new TableDefinition( tableName, tableNameAlias, tableId, colsIsStr, cols, aliasCols, tempAliasCols, 
					pkPlain, pkAlias, pkTempAlias);
		} catch (SQLException e) {
			throw new ScratchpadException(e);
		}
	}

	/**
	 * Execute select operation in the temporary table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @param tables 
	 * @return
	 * @throws SQLException
	 * @throws ScratchpadException 
	 */
	public Result executeTempOpSelect( DBOperation op, Select dbOp, IDBScratchpad db, ExecutionPolicy[] policies, String[][] tables) throws SQLException, ScratchpadException {
		Debug.println( ">>" + dbOp);
		HashMap<String,Integer> columnNamesToNumbersMap = new HashMap<String,Integer>();
		StringBuffer buffer = new StringBuffer();
		buffer.append("select ");						// select in base table
		PlainSelect select = (PlainSelect)dbOp.getSelectBody();
		List what = select.getSelectItems();
		int colIndex=1;
		TableDefinition tabdef;
		if( what.size() == 1 && what.get(0).toString().equalsIgnoreCase("*")) {
//			buffer.append( "*");
			for( int i = 0; i < policies.length; i++) {
				tabdef = policies[i].getTableDefinition();
				tabdef.addAliasColumnList(buffer, tables[i][1]);
				for(int j=0;j<tabdef.colsPlain.length;j++)
					columnNamesToNumbersMap.put(tabdef.colsPlain[j], colIndex++);
			}
		} else {
			Iterator it = what.iterator();
			String str;
			while( it.hasNext()) {
				str = it.next().toString();
				int starPos = str.indexOf(".*");
				if( starPos != -1) {
					String itTable = str.substring(0, starPos).trim();
					for( int i = 0; i < tables.length; ) {
						if( itTable.equalsIgnoreCase(tables[i][0]) || itTable.equalsIgnoreCase(tables[i][1])) {
							tabdef = policies[i].getTableDefinition();
							tabdef.addAliasColumnList(buffer, tables[i][1]);
							break;
						}
						i++;
						if( i == tables.length) {
							Debug.println( "not expected " + str + " in select");
							buffer.append(str);
							buffer.append( ",");
						}
					}
				} else {
					buffer.append(str);
					buffer.append( ",");
				}
				columnNamesToNumbersMap.put(str, colIndex++);
			}
		}
		for( int i = 0; i < policies.length; i++) {
			if( i > 0)
				buffer.append( ",");
			policies[i].addKeyVVBothTable( buffer, tables[i][1]);
		}
		buffer.append( " from ");
		for( int i = 0; i < policies.length; i++) {
			if( i > 0)
				buffer.append( ",");
			policies[i].addFromTable( buffer, ! db.isReadOnly(), tables[i]);
		}
		addWhere( buffer, select.getWhere(), policies, tables);
		addGroupBy( buffer, select.getGroupByColumnReferences(), policies, tables);
		addOrderBy( buffer, select.getOrderByElements(), policies, tables);
		int nResults = addLimit( buffer, select.getLimit());
		
		Debug.println( "---->" + buffer.toString());
		//System.err.println( "---->" + buffer.toString());
		List<String[]> result = new ArrayList<String[]>();
		ResultSet rs = db.executeQuery( buffer.toString());
		addToResultList(rs, result, db, policies, nResults);
		rs.close();
		return DBSelectResult.createResult( result,columnNamesToNumbersMap);
			
	}
	
	/**
	 * Execute select operation in the temporary table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @return
	 * @throws SQLException
	 * @throws ScratchpadException 
	 */
	public Result executeTempOpSelect( DBOperation op, Select dbOp, IDBScratchpad db, String[] table) throws SQLException, ScratchpadException {
		Debug.println( ">>" + dbOp);
		HashMap<String,Integer> columnNamesToNumbersMap = new HashMap<String,Integer>();

		if( db.isReadOnly()) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("select ");
			PlainSelect select = (PlainSelect)dbOp.getSelectBody();
			List what = select.getSelectItems();
			if( what.size() == 1 && what.get(0).toString().equalsIgnoreCase("*")) {
				buffer.append( def.getPlainColumnList());
//				buffer.append( "*");
				buffer.append( ",");
			} else {
				Iterator it = what.iterator();
				int colNumber=1;
				String str;
				while( it.hasNext()) {
					str = it.next().toString();
					int starPos = str.indexOf(".*");
					if( starPos != -1) {
						buffer.append( def.getPlainColumnList());
					} else 
						buffer.append(str);
					buffer.append( ",");
					columnNamesToNumbersMap.put(str, colNumber++);
				}
			}
			buffer.append( def.getPkListPlain());
			buffer.append( ",");
			buffer.append( DBScratchpad.SCRATCHPAD_COL_VV);
			buffer.append( " from ");
			buffer.append( table[0]);
			addWhere( buffer, select.getWhere());
			addGroupBy( buffer, select.getGroupByColumnReferences());
			addOrderBy( buffer, select.getOrderByElements());
			int nResults = addLimit( buffer, select.getLimit());
			
			Debug.println( "---->" + buffer.toString());
			List<String[]> result = new ArrayList<String[]>();
			ResultSet rs = db.executeQuery( buffer.toString());
			addToResultList(rs, result, db, nResults);
			rs.close();
			return DBSelectResult.createResult(result,columnNamesToNumbersMap);
		} else {
			StringBuffer buffer = new StringBuffer();
			buffer.append("select ");
			PlainSelect select = (PlainSelect)dbOp.getSelectBody();
			List what = select.getSelectItems();
			if( what.size() == 1 && what.get(0).toString().equalsIgnoreCase("*")) {
				buffer.append( def.getPlainColumnList());
				buffer.append( ",");
			} else {
				Iterator it = what.iterator();
				while( it.hasNext()) {
					buffer.append( it.next());
					buffer.append( ",");
				}
			}
			buffer.append( def.getPkListPlain());
			buffer.append( ",");
			buffer.append( DBScratchpad.SCRATCHPAD_COL_VV);
			buffer.append( " from ");
			buffer.append( table[0]);
			addWhere( buffer, select.getWhere());
			addGroupBy( buffer, select.getGroupByColumnReferences());
			addOrderBy( buffer, select.getOrderByElements());
			int nResults = addLimit( buffer, select.getLimit());
			
			Debug.println( "---->" + buffer.toString());
			List<String[]> result = new ArrayList<String[]>();
			ResultSet rs = db.executeQuery( buffer.toString());
			addToResultList(rs, result, db, nResults);
			rs.close();
			return DBSelectResult.createResult( result);
			
		}
	}

	public Result executeTemporaryQuery(DBSingleOperation dbOp, IDBScratchpad db, String[] tables) throws SQLException, ScratchpadException {
		if( dbOp.getStatementObj() instanceof Select)
			return executeTempOpSelect( dbOp, (Select)dbOp.getStatementObj(), db, tables);
		throw new ScratchpadException( "Unknown update operation : " + dbOp.toString());
	}

	@Override
	public Result executeTemporaryQuery( DBSingleOperation dbOp, IDBScratchpad db, ExecutionPolicy[] policies, String[][] tables) throws SQLException, ScratchpadException {
		if( dbOp.getStatementObj() instanceof Select)
			return executeTempOpSelect( dbOp, (Select)dbOp.getStatementObj(), db, policies, tables);
		throw new ScratchpadException( "Unknown update operation : " + dbOp.toString());
	}

	/**
	 * Execute insert operation in the temporary table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @return
	 * @throws SQLException
	 */
	protected DBUpdateResult executeTempOpInsert(DBOperation op, Insert dbOp, IDBScratchpad db) throws SQLException {
		Debug.println( ">>" + dbOp);
		
		StringBuffer buffer = new StringBuffer();
		buffer.append( "insert into ");
		buffer.append( dbOp.getTable().getName());
		List s = dbOp.getColumns();
		if( s == null) {
			buffer.append( "(");
			buffer.append(def.getPlainColumnList());
			buffer.append( ")");
		} else {
			buffer.append( "(");
			Iterator it = s.iterator();
			boolean first = true;
			while( it.hasNext()) {
				if( ! first)
					buffer.append(",");
				first = false;
				buffer.append(it.next());
			}
			buffer.append( ")");
		}
		buffer.append( " values ");
		buffer.append( dbOp.getItemsList());

		// TODO: blue transactions need to fail here when the value inserted already exists
		Debug.println( buffer.toString());
		int result = db.executeUpdate( buffer.toString());
		String[] pkVal = def.getPlainPKValue( dbOp.getColumns(), dbOp.getItemsList());
		db.addToWriteSet( DBWriteSetEntry.createEntry( dbOp.getTable().toString(), pkVal, this.blue));
		//db.addToOpLog( new DBOpPair( op, pkVal));
		return DBUpdateResult.createResult( result);
	}

	/**
	 * Execute delete operation in the temporary table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @return
	 * @throws SQLException
	 */
	protected DBUpdateResult executeTempOpDelete(DBOperation op, Delete dbOp, IDBScratchpad db) throws SQLException {
		Debug.println( ">>" + dbOp);
		
        // GET PRIMERY KEY VALUE
		StringBuffer buffer = new StringBuffer();
		buffer.append( "select ");
		buffer.append( def.getPkListPlain());
		buffer.append( " from ");
		buffer.append( def.name);
		addWhere( buffer, dbOp.getWhere());
		
		Debug.println( ":" + buffer.toString());
		ResultSet res = db.executeQuery( buffer.toString());
		while( res.next()) {
			int nPks = def.getPksPlain().length;
			String[] pkVal = new String[nPks];
			for( int i = 0; i < pkVal.length; i++)
				pkVal[i] = res.getObject(i+1).toString();
			db.addToWriteSet( DBWriteSetEntry.createEntry( dbOp.getTable().toString(), pkVal, this.blue));
			//db.addToOpLog( new DBOpPair( op, pkVal));
		}
		res.close();
		buffer = new StringBuffer();
		buffer.append( "update ");
		buffer.append( def.name);
		buffer.append( " set ");
		buffer.append( DBScratchpad.SCRATCHPAD_COL_DELETED);
		buffer.append( " = TRUE ");
		addWhere( buffer, dbOp.getWhere());
		
		Debug.println( ":" + buffer.toString());
		int result = db.executeUpdate( buffer.toString());
		return DBUpdateResult.createResult( result);
	}

	/**
	 * Execute update operation in the temporary table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @return
	 * @throws SQLException
	 */
	protected DBUpdateResult executeTempOpUpdate(DBOperation op, Update dbOp, IDBScratchpad db) throws SQLException {
		Debug.println( ">>" + dbOp.toString());

        // COPY ROWS TO MODIFY TO SCRATCHPAD
		StringBuffer buffer = new StringBuffer();
		buffer.append( "select * from ");
		buffer.append( def.name);
		addWhere( buffer, dbOp.getWhere());
		Debug.println( ":" + buffer.toString());
		ResultSet res = db.executeQuery( buffer.toString());
		while( res.next()) {
			int nPks = def.getPksPlain().length;
			String[] pkVal = new String[nPks];
			for( int i = 0; i < pkVal.length; i++)
				pkVal[i] = res.getObject(i+1).toString();
			db.addToWriteSet( DBWriteSetEntry.createEntry( dbOp.getTable().toString(), pkVal, this.blue));
			//db.addToOpLog( new DBOpPair( op, pkVal));
		}
		res.close();

		// DO THE UPDATE
		buffer.setLength(0);
		buffer.append( "update ");
		buffer.append( def.name);
		buffer.append( " set ");
		Iterator colIt = dbOp.getColumns().iterator();
		Iterator expIt = dbOp.getExpressions().iterator();
		while( colIt.hasNext()) {
			buffer.append( colIt.next());
			buffer.append( " = ");
			buffer.append( expIt.next());
			if( colIt.hasNext())
				buffer.append( " , ");
		}
		buffer.append( " where ");
		buffer.append( dbOp.getWhere().toString());

		Debug.println( ":" + buffer.toString());
		int result = db.executeUpdate( buffer.toString());
		return DBUpdateResult.createResult( result);
	}

	@Override
	public Result executeTemporaryUpdate(DBSingleOperation dbOp, IDBScratchpad db) throws SQLException, ScratchpadException {
		if( dbOp.getStatementObj() instanceof Insert)
			return executeTempOpInsert( dbOp, (Insert)dbOp.getStatementObj(), db);
		if( dbOp.getStatementObj() instanceof Delete)
			return executeTempOpDelete( dbOp, (Delete)dbOp.getStatementObj(), db);
		if( dbOp.getStatementObj() instanceof Update)
			return executeTempOpUpdate( dbOp, (Update)dbOp.getStatementObj(), db);
		throw new ScratchpadException( "Unknown update operation : " + dbOp.toString());
	}
	/**
	 * Execute insert operation in the final table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @param b 
	 * @return
	 * @throws SQLException
	 */
	protected abstract void executeDefOpInsert(DBOpPair op, Insert dbOp, IDBScratchpad db, LogicalClock lc, TimeStamp ts, boolean b) throws SQLException;
	/**
	 * Execute delte operation in the final table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @param b 
	 * @return
	 * @throws SQLException
	 */
	protected abstract void executeDefOpDelete(DBOpPair op, Delete dbOp, IDBScratchpad db, LogicalClock lc, TimeStamp ts, boolean b) throws SQLException;
	/**
	 * Execute update operation in the final table
	 * @param op
	 * @param dbOp
	 * @param db
	 * @param b 
	 * @ptama b First call to this table
	 * @return
	 * @throws SQLException
	 */
	protected abstract void executeDefOpUpdate(DBOpPair op, Update dbOp, IDBScratchpad db, LogicalClock lc, TimeStamp ts, boolean b) throws SQLException;

	@Override
	public void executeDefiniteUpdate(DBSingleOpPair op, IDBScratchpad db, LogicalClock lc, TimeStamp ts, boolean b) throws SQLException {
		if( op.op.getStatementObj() instanceof Insert)
			executeDefOpInsert( op, (Insert)op.op.getStatementObj(), db, lc, ts, b);
		else if( op.op.getStatementObj() instanceof Delete)
			executeDefOpDelete( op, (Delete)op.op.getStatementObj(), db, lc, ts, b);
		else if( op.op.getStatementObj() instanceof Update)
			executeDefOpUpdate( op, (Update)op.op.getStatementObj(), db, lc, ts, b);
		else
			throw new RuntimeException( "Not expected:" + op.op.getStatement());
	}


}
