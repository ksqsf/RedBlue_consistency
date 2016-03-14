package applications.microbenchmark.TxMudTest;
import util.Debug;

import java.sql.*;

import txstore.scratchpad.*;
import txstore.scratchpad.rdbms.DBScratchpad;

public class InitDB
{
	protected Connection conn;
	protected Statement stat;
	protected String url, user, pwd;
	
	public InitDB( String url, String user, String pwd) throws SQLException {
		this.url = url;
		this.user = user;
		this.pwd = pwd;
		init();
	}
	
	protected void init() throws SQLException {
		conn = DriverManager.getConnection( url, user, pwd);
		conn.setAutoCommit(true);
		stat = conn.createStatement();
		
	}
	
	protected void createTables() throws SQLException {
		try {
			stat.execute( "DROP TABLE IF EXISTS " + DBScratchpad.SCRATCHPAD_PREFIX );
		} catch (SQLException e) {
			// do nothing
		}
		try {
			stat.execute( "DROP TABLE IF EXISTS t1");
		} catch (SQLException e) {
			// do nothing
		}
		stat.execute( "CREATE TABLE t1 ("+
				"a int(10) NOT NULL primary key," +
				"b int(10) ," + 
				"c int(10) ," + 
				"d int(10) ," +
				"e varchar(50)," + 
				//DBScratchpad.SCRATCHPAD_COL_PREFIX + "del bool default false," + //Can not work since bool is not support by mimer
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "ts int default 0,"+
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "clock varchar(100)" + 
				")");
		try {
			stat.execute( "DROP TABLE IF EXISTS t2");
		} catch (SQLException e) {
			// do nothing
		}
		stat.execute( "CREATE TABLE t2 ("+
				"a int(10) NOT NULL primary key," +
				"b int(10) ," + 
				"c int(10) ," + 
				"d int(10) ," +
				"e varchar(50)," + 
				//DBScratchpad.SCRATCHPAD_COL_PREFIX + "del bool default false," + 
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "ts int default 0,"+
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "clock varchar(100)" + 
				")");
		try {
			stat.execute( "DROP TABLE IF EXISTS t3");
		} catch (SQLException e) {
			// do nothing
		}
		stat.execute( "CREATE TABLE t3 ("+
				"a int(10) NOT NULL primary key," +
				"b int(10) ," + 
				"c int(10) ," + 
				"d int(10) ," +
				"e varchar(50)," + 
				//DBScratchpad.SCRATCHPAD_COL_PREFIX + "del bool default false," + 
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "ts int default 0,"+
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "clock varchar(100)" + 
				")");
		try {
			stat.execute( "DROP TABLE IF EXISTS t4");
		} catch (SQLException e) {
			// do nothing
		}
		stat.execute( "CREATE TABLE t4 ("+
				"a int(10) NOT NULL," +
				"b int(10) ," + 
				"c int(10) ," + 
				"d int(10) ," +
				"e varchar(50) not null," + 
				//DBScratchpad.SCRATCHPAD_COL_PREFIX + "del bool default false," + 
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "ts int default 0,"+
				DBScratchpad.SCRATCHPAD_COL_PREFIX + "clock varchar(100)," + 
				" primary key (a,e)" + 
				")");
		
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Class.forName("com.mimer.jdbc.Driver");
			String url = "jdbc:mimer://localhost:13600/micro";
		String user = "SYSADM";
		String pwd = "123456";
		if( args.length > 0)
			url = args[0];
		if( args.length > 1)
			user = args[1];
		if( args.length > 2)
			pwd = args[2];
		
		InitDB db = new InitDB( url, user, pwd); 
		
		db.createTables();
		} catch( Exception e) {
			e.printStackTrace();
		}

	}

}
