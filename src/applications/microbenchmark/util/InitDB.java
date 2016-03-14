package applications.microbenchmark.util;

import java.sql.*;
import java.util.Random;


public class InitDB
{
	protected Connection conn;
	protected Statement stat;
	protected static String host;
	protected static int port;
	protected String url;
	protected static String user;
	protected static String pwd;
	protected String logicalClockStr;
	static final String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static Random randomGenerator;
	static int recordNum;
	static int dcNum;
	static int tableNum;
	
	public InitDB( String url, String user, String pwd) throws SQLException {
		this.url = url;
		this.user = user;
		this.pwd = pwd;
		randomGenerator = new Random();
		init();
	}
	
	protected void setLogicalClock(int dcNum){
		logicalClockStr="";
		for(int i = 0; i < dcNum; i++){
			logicalClockStr +="0-";
		}
		logicalClockStr +="0";
	}
	
	public String get_random_string(int length) {
		StringBuilder rndString = new StringBuilder(length);
		for(int i = 0;i < length; i ++){
			rndString.append(charSet.charAt(randomGenerator.nextInt(charSet.length())));
		}
		return rndString.toString();
	}
	
	protected void init() throws SQLException {
		conn = DriverManager.getConnection( url, user, pwd);
		conn.setAutoCommit(false);
		stat = conn.createStatement();
		
	}
	
	protected void createDB(){
		try{
			stat.execute("DROP DATABASE IF EXISTS micro");
		}catch(SQLException e){
			e.printStackTrace();
		}
		try {
			stat.execute("CREATE DATABASE micro");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			stat.execute("use micro;");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			conn.commit();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	protected void createScratchpadTable() throws SQLException{
		stat.execute("use micro");
	    stat.execute("CREATE TABLE IF NOT EXISTS SCRATCHPAD_ID ( k int NOT NULL primary key, id int);");
	    stat.execute("INSERT INTO SCRATCHPAD_ID VALUES ( 1, 1);");
	    stat.execute("CREATE TABLE IF NOT EXISTS SCRATCHPAD_TRX ( k int NOT NULL primary key, id int);");
	    stat.execute("INSERT INTO SCRATCHPAD_TRX VALUES ( 1, 1);"); 
	    conn.commit();
	}

	protected void createTables() throws SQLException {
		for(int i = 1; i <= tableNum; i++){
			try {
				stat.execute( "DROP TABLE IF EXISTS t"+i+";");
			} catch (SQLException e) {
				// do nothing
			}
			stat.execute( "CREATE TABLE t"+i+" ("+
					"a int(10) NOT NULL primary key," +
					"b int(10) unsigned," + 
					"c int(10) unsigned," + 
					"d int(10) unsigned," +
					"e varchar(50)," + 
					"_SP_del BIT(1) default false," + 
					"_SP_ts int default 0,"+
					"_SP_clock varchar(100)" + 
					");");
		}
		conn.commit();
		
	}
	
	protected void insertIntoTables() throws SQLException{
		for (int i =0;i < recordNum; i ++){
		int a = i;
		int b = randomGenerator.nextInt(recordNum)+1;
		int c = randomGenerator.nextInt(recordNum)+1;
		int d = randomGenerator.nextInt(recordNum)+1;
		String e = get_random_string(50) ;
		for(int j =1 ;j <= tableNum; j++){
			stat.execute("insert into t"+j+" values (" + Integer.toString(a) + "," + Integer.toString(b) + "," +
			Integer.toString(c) + "," + Integer.toString(d) + ",'" + e + "'," + Integer.toString(0) + "," + Integer.toString(0) + ",'" +logicalClockStr + "')");
			}
		}
		conn.commit();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 7) {
			System.out.println("InitDB host port user pwd recordNum dcNum tableNum");
			System.exit(-1);
		}
		host = args[0];
		port = Integer.parseInt(args[1]);
		user = args[2];
		pwd = args[3];
		recordNum = Integer.parseInt(args[4]);
		dcNum = Integer.parseInt(args[5]);
		tableNum = Integer.parseInt(args[6]);
		try {
			Class.forName("com.mysql.jdbc.Driver");
		String url = "jdbc:mysql://"+host+":"+port;
		
		InitDB db = new InitDB( url, user, pwd); 
		db.setLogicalClock(dcNum);
		db.createDB();
		db.createScratchpadTable();
		db.createTables();
		db.insertIntoTables();
		} catch( Exception e) {
			e.printStackTrace();
		}

	}

}
