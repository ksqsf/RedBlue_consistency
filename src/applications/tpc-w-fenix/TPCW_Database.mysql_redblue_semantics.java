/* 
 * TPCW_Database.java - Contains all of the code involved with database
 *                      accesses, including all of the JDBC calls. These
 *                      functions are called by many of the servlets.
 *
 ************************************************************************
 *
 * This is part of the the Java TPC-W distribution,
 * written by Harold Cain, Tim Heil, Milo Martin, Eric Weglarz, and Todd
 * Bezenek.  University of Wisconsin - Madison, Computer Sciences
 * Dept. and Dept. of Electrical and Computer Engineering, as a part of
 * Prof. Mikko Lipasti's Fall 1999 ECE 902 course.
 *
 * Copyright (C) 1999, 2000 by Harold Cain, Timothy Heil, Milo Martin, 
 *                             Eric Weglarz, Todd Bezenek.
 *
 * This source code is distributed "as is" in the hope that it will be
 * useful.  It comes with no warranty, and no author or distributor
 * accepts any responsibility for the consequences of its use.
 *
 * Everyone is granted permission to copy, modify and redistribute
 * this code under the following conditions:
 *
 * This code is distributed for non-commercial use only.
 * Please contact the maintainer for restrictions applying to 
 * commercial use of these tools.
 *
 * Permission is granted to anyone to make or distribute copies
 * of this code, either as received or modified, in any
 * medium, provided that all copyright notices, permission and
 * nonwarranty notices are preserved, and that the distributor
 * grants the recipient permission for further redistribution as
 * permitted by this document.
 *
 * Permission is granted to distribute this code in compiled
 * or executable form under the same conditions that apply for
 * source code, provided that either:
 *
 * A. it is accompanied by the corresponding machine-readable
 *    source code,
 * B. it is accompanied by a written offer, with no time limit,
 *    to give anyone a machine-readable copy of the corresponding
 *    source code in return for reimbursement of the cost of
 *    distribution.  This written offer must permit verbatim
 *    duplication by anyone, or
 * C. it is distributed by someone who received only the
 *    executable form, and is accompanied by a copy of the
 *    written offer of source code that they received concurrently.
 *
 * In other words, you are welcome to use, share and improve this codes.
 * You are forbidden to forbid anyone else to use, share and improve what
 * you give them.
 *
 ************************************************************************
 *
 * Changed 2003 by Jan Kiefer.
 *
 ************************************************************************/

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.lang.Math.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;

import applications.microbenchmark.TxMudTest.ExecuteScratchpadFactory;


//scratchpad
import txstore.proxy.ClosedLoopProxy;
import txstore.scratchpad.ScratchpadConfig;
import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.jdbc.PassThroughProxy;
import txstore.scratchpad.rdbms.jdbc.TxMudDriver;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.jdbc.TxMudConnection;

//txmud
import network.ParallelPassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;
import txstore.proxy.ApplicationInterface;
import txstore.proxy.ClosedLoopProxy;
import txstore.util.Operation;
import txstore.scratchpad.rdbms.util.tpcw.*;

import util.Debug;

public class TPCW_Database {
    private static Void VOID = null;

    static String driver = "@jdbc.driver@";
	static String jdbcPath = "@jdbc.path@";
	static String actualDriver = "@jdbc.actualDriver@";
	static String jdbcActualPath = "@jdbc.actualPath@";
	static String padClass = "@jdbc.scratchpadClass@";
	static String user = "@jdbc.user@";
	static String password = "@jdbc.password@";
    
    // Pool of *available* connections.
    static Vector availConn = new Vector(0);
    static int checkedOut = 0;
    static int totalConnections = 0;
    static int createdConnections = 0;
    static int closedConnections = 0;
    static AtomicInteger aborts =new AtomicInteger(0);
    static AtomicInteger commitedtxn =new AtomicInteger(0);
    static int proxyId=0;
    static int totalproxies=1;
    static int transactions = 0;
    static long startmi=0;
    static long endmi=0;
    static boolean pool_initialized=false;
    public static TPCW_TxMud_Proxy proxy;
    public static AtomicInteger ShoppingIDFactory=new AtomicInteger(0);
    public static AtomicInteger OrderLineIDFactory=new AtomicInteger(0);
    public static AtomicInteger AddrIDFactory=new AtomicInteger(0);
    public static AtomicInteger CustomerIDFactory=new AtomicInteger(0);
    //    private static final boolean use_connection_pool = false;
    private static final boolean use_connection_pool = true;
    public static int maxConn = @jdbc.connPoolMax@;
    
    public static Random doubleGenerator = new Random();
    
    // Here's what the db line looks like for postgres
    //public static final String url = "jdbc:postgresql://eli.ece.wisc.edu/tpcwb";

    
    // Get a connection from the pool.
    public static synchronized TxMudConnection getConnection() {
    	//count only transactions within measurement interval
    	//System.err.println("get new TxMud Connection\n");
    	long time = System.currentTimeMillis();
    	if( time > startmi && time < endmi ) transactions++;
    	//
    //System.err.println("INFO: connect to database");
	if (!use_connection_pool) {
	    return getNewConnection();
	} else {
		TxMudConnection con = null;
	    while (availConn.size() > 0) {
				// Pick the first Connection in the Vector
				// to get round-robin usage
		con = (TxMudConnection) availConn.firstElement();
		availConn.removeElementAt(0);
		try {
		    if (con.isClosed()) {
			continue;
		    }
		}
		catch (SQLException e) {
		    e.printStackTrace();
		    System.err.println("ERROR: connect to database - maybe there's no connection available: "+availConn.size());
		    continue;
		}
		
		// Got a connection.
		checkedOut++;
		try { con.setAutoCommit(false); } catch (SQLException e) { 
			System.err.println("ERROR: connect to database - maybe there's no connection available: "+availConn.size());
			e.printStackTrace();
			return null; }
		return(con);
	    }
	    
	    if (maxConn == 0 || checkedOut < maxConn) {
		con = getNewConnection();	
		totalConnections++;
	    }
	    
	    if (con != null) {
	    	checkedOut++;
	    	try { con.setAutoCommit(false); } catch (SQLException e) { 
	    		e.printStackTrace();
	    	return null; 
	    	}
	    }
	    
	    
	    return con;
	}
    }
    
    // Return a connection to the pool.
    public static synchronized void returnConnection(TxMudConnection con)
    throws java.sql.SQLException
    {
    	//System.err.println("INFO: add avaiable connection back to the pool, total available connections: "+availConn.size());
	if (!use_connection_pool) {
	    con.close();
	} else {
	    checkedOut--;
	    availConn.addElement(con);
	}
		//System.err.println("INFO: add avaiable connection back to the pool. total available connecitons: "+availConn.size());
    }

    // Get a new connection to DB2
    public static TxMudConnection getNewConnection() {
    	System.out.println("INFO: creating new connection");
		System.out.println("INFO: configuring txmud: " +
				"actualDriver <"+actualDriver+"> jdbcActualpath <jdbcActualPath>"+
				"username:<"+user+"> password:<"+password+"> padClass: <"+padClass+">");
	try {
	    //Class.forName(driver);
		Class.forName("txstore.scratchpad.rdbms.jdbc.TxMudDriver");
	    // Class.forName("postgresql.Driver");

	    // Create URL for specifying a DBMS
	    TxMudConnection con;
	    while(true) {
		try {
		    //   con = DriverManager.getConnection("jdbc:postgresql://eli.ece.wisc.edu/tpcw", "milo", "");
		    //con = (TxMudConnection) DriverManager.getConnection(jdbcPath);
			con = (TxMudConnection) DriverManager.getConnection("jdbc:txmud:test");
		    break;  
		} catch (java.sql.SQLException ex) {
		    System.err.println("Error getting connection: " + 
				       ex.getMessage() + " : " +
				       ex.getErrorCode() + 
				       ": trying to get connection again.");
		    ex.printStackTrace();
		    java.lang.Thread.sleep(1000);
		}
	    }
	    con.setAutoCommit(false);
	    createdConnections++;
	    return con;
	} catch (java.lang.Exception ex) {
		//System.err.println("Error: loading scratchpad driver: <"+driver+">");
	    ex.printStackTrace();
	}
	return null;
    }

    public static <T> T withTransaction(tx.TransactionalCommand<T> command) {
	try {
	    while (true) {
	    long time = System.currentTimeMillis();	
		TxMudConnection con = getConnection();
		if (con==null) {
			System.err.println("ERROR: Restarting TX - because there's no database connection available!! you should change system parameter!! this is not an abort");
			continue;
		}
		boolean txFinished = false;
		try {
		    T result = command.doIt(con);
		    con.commit();
			if( time > startmi && time < endmi )
				commitedtxn.incrementAndGet();
		    returnConnection(con);
		    txFinished = true;
		    return result;
		} catch (SQLException sqle) {
			if( time > startmi && time < endmi )	
				aborts.incrementAndGet();
		    System.err.println("Restarting TX because of a database problem (hopefully just a conflict) total aborts within the MI so far:"+aborts);
		    sqle.printStackTrace();
		    //con.rollback();
		    returnConnection(con);
		    txFinished = true;
		}catch(Exception spde){
		 	if( time > startmi && time < endmi )	
				aborts.incrementAndGet();
		    System.err.println("Restarting TX because of a database problem (hopefully just a conflict) total aborts within the MI so far:"+aborts);
		    spde.printStackTrace();
		    //con.rollback();
		    returnConnection(con);
		    txFinished = true;
		} 
		finally {
		    if (!txFinished) {
			//con.rollback();
			returnConnection(con);
		    }
		}
	    }
	} catch (SQLException sqle) {
	    // exception occurred either rolling back or releasing resources.  Not much we can do here
		System.err.println("----------------------------------------------------------------------------");
		System.err.println("A very strange error happened!! - failed during rollback! Aborts so far:"
				+aborts+" available connections:"+availConn.size()+" total connections:"+maxConn);
		System.out.println("A very strange error happened!! - failed during rollback! Aborts so far:"
				+aborts+" available connections:"+availConn.size()+" total connections:"+maxConn);
		sqle.printStackTrace();
		System.err.println("----------------------------------------------------------------------------");
	    throw new RuntimeException(sqle);
	}
    }

    public static String[] getName(final int c_id) {
	String[] name = withTransaction(new tx.TransactionalCommand<String[]>() {
		public String[] doIt(Connection con) throws SQLException {
		    return getName((TxMudConnection)con, c_id);
		}
	    });
	return name;
    }
    private static String[] getName(TxMudConnection con, int c_id) throws SQLException {
	String name[] = new String[2];
	    PreparedStatement get_name = con.prepareStatement
		(@sql.getName@);
	    
	    // Set parameter
	    get_name.setInt(1, c_id);
	    // 	    out.println("About to execute query!");
	    //            out.flush();

	    ResultSet rs = get_name.executeQuery();
	    
	    // Results
	    boolean value_ok = rs.next();
	    if(!value_ok)
	    	System.err.println("Error, query returned an empty set: "+@sql.getName@+" value:"+c_id);
	    name[0] = rs.getString("c_fname");
	    name[1] = rs.getString("c_lname");
	    rs.close();
	    get_name.close();
	    try{
	    	Debug.println("Set empty shadow op for getName");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return name;
    }

    public static Book getBook(final int i_id) {
	Book book = withTransaction(new tx.TransactionalCommand<Book>() {
		public Book doIt(Connection con) throws SQLException {
		    return getBook((TxMudConnection)con, i_id);
		}
	    });
	return book;
    }
    private static Book getBook(TxMudConnection con, int i_id) throws SQLException {
	Book book = null;
	    PreparedStatement statement = con.prepareStatement
		(@sql.getBook@);
	    
	    // Set parameter
	    statement.setInt(1, i_id);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    rs.next();
	    book = new Book(rs);
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getBook");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return book;
    }

    public static Customer getCustomer(final String UNAME){
	Customer customer = withTransaction(new tx.TransactionalCommand<Customer>() {
		public Customer doIt(Connection con) throws SQLException {
		    return getCustomer((TxMudConnection)con, UNAME);
		}
	    });
	return customer;
    }
    private static Customer getCustomer(TxMudConnection con, String UNAME) throws SQLException {
	Customer cust = null;
	    PreparedStatement statement = con.prepareStatement
		(@sql.getCustomer@);
	    
	    // Set parameter
	    statement.setString(1, UNAME);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    if(rs.next())
		cust = new Customer(rs);
	    else {
		System.err.println("ERROR: NULL returned in getCustomer! "+UNAME+" sql:"+@sql.getCustomer@);
		rs.close();
		statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getCustomer null");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	    }
	    
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getCustomer not null");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return cust;
    }

    public static Vector doSubjectSearch(final String search_key) {
	Vector Vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doSubjectSearch((TxMudConnection)con, search_key);
		}
	    });
	return Vector;
    }
    private static Vector doSubjectSearch(TxMudConnection con, String search_key) throws SQLException {
	Vector vec = new Vector();
	    PreparedStatement statement = con.prepareStatement
		(@sql.doSubjectSearch@);
	    
	    // Set parameter
	    statement.setString(1, search_key);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    while(rs.next()) {
		vec.addElement(new Book(rs));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for doSubjectSearch");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return vec;	
    }

    public static Vector doTitleSearch(final String search_key) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doTitleSearch((TxMudConnection)con, search_key);
		}
	    });
	return vector;
    }
    private static Vector doTitleSearch(TxMudConnection con, String search_key) throws SQLException {
	Vector vec = new Vector();
	    PreparedStatement statement = con.prepareStatement
		(@sql.doTitleSearch@);
	    
	    // Set parameter
	    statement.setString(1, search_key+"%");
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    while(rs.next()) {
		vec.addElement(new Book(rs));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for doTitleSearch");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return vec;	
    }

    public static Vector doAuthorSearch(final String search_key) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doAuthorSearch((TxMudConnection)con, search_key);
		}
	    });
	return vector;
    }
    private static Vector doAuthorSearch(TxMudConnection con, String search_key) throws SQLException {
	Vector vec = new Vector();
	    PreparedStatement statement = con.prepareStatement
		(@sql.doAuthorSearch@);

	    // Set parameter
	    statement.setString(1, search_key+"%");
	    ResultSet rs = statement.executeQuery();

	    // Results
	    while(rs.next()) {
		vec.addElement(new Book(rs));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for doAuthorSearch");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return vec;	
    }

    public static Vector getNewProducts(final String subject) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return getNewProducts((TxMudConnection)con, subject);
		}
	    });
	return vector;
    }
    private static Vector getNewProducts(TxMudConnection con, String subject) throws SQLException {
	Vector vec = new Vector();  // Vector of Books
	    PreparedStatement statement = con.prepareStatement
		(@sql.getNewProducts@);

	    // Set parameter
	    statement.setString(1, subject);
	    ResultSet rs = statement.executeQuery();

	    // Results
	    while(rs.next()) {
		vec.addElement(new ShortBook(rs));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getNewProducts");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return vec;	
    }

    public static Vector getBestSellers(final String subject) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return getBestSellers((TxMudConnection)con, subject);
		}
	    });
	return vector;
    }
    private static Vector getBestSellers(TxMudConnection con, String subject) throws SQLException {
	Vector vec = new Vector();  // Vector of Books
	    //The following is the original, unoptimized best sellers query.
	    PreparedStatement statement = con.prepareStatement
		(@sql.getBestSellers@);
	    //This is Mikko's optimized version, which depends on the fact that
	    //A table named "bestseller" has been created.
	    /*PreparedStatement statement = con.prepareStatement
		("SELECT bestseller.i_id, i_title, a_fname, a_lname, ol_qty " + 
		 "FROM item, bestseller, author WHERE item.i_subject = ?" +
		 " AND item.i_id = bestseller.i_id AND item.i_a_id = author.a_id " + 
		 " ORDER BY ol_qty DESC FETCH FIRST 50 ROWS ONLY");*/
	    
	    // Set parameter
	    statement.setString(1, subject);
	    ResultSet rs = statement.executeQuery();

	    // Results
	    while(rs.next()) {
		vec.addElement(new ShortBook(rs));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getBestSeller");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return vec;	
    }

    public static void getRelated(final int i_id, final Vector i_id_vec, final Vector i_thumbnail_vec) {
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    getRelated((TxMudConnection)con, i_id, i_id_vec, i_thumbnail_vec);
		    return VOID;
		}
	    });
    }
    private static void getRelated(TxMudConnection con, int i_id, Vector i_id_vec, Vector i_thumbnail_vec) throws SQLException {
	    PreparedStatement statement = con.prepareStatement
		(@sql.getRelated@);

	    // Set parameter
	    statement.setInt(1, i_id);
	    ResultSet rs = statement.executeQuery();

	    // Clear the vectors
	    i_id_vec.removeAllElements();
	    i_thumbnail_vec.removeAllElements();

	    // Results
	    while(rs.next()) {
		i_id_vec.addElement(new Integer(rs.getInt(1)));
		i_thumbnail_vec.addElement(rs.getString(2));
	    }
	    rs.close();
	    statement.close();
	    try{
	    	Debug.println("Set empty shadow op for getRelated");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void adminUpdate(final int i_id, final double cost, final String image, final String thumbnail) {
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    adminUpdate((TxMudConnection)con, i_id, cost, image, thumbnail);
		    return VOID;
		}
	    });
    }
    private static void adminUpdate(TxMudConnection con, int i_id, double cost, String image, String thumbnail) throws SQLException {
	    PreparedStatement statement = con.prepareStatement
		(@sql.adminUpdate.txmud@);

	    // Set parameter
	    statement.setDouble(1, cost);
	    statement.setString(2, image);
	    statement.setString(3, thumbnail);
	    
	    Calendar calendar = Calendar.getInstance();
	    long pubdate = calendar.getTimeInMillis();
	    statement.setDate(4, new java.sql.Date(pubdate));
	    	    
	    statement.setInt(5, i_id);
	    statement.executeUpdate();
	    statement.close();
	    PreparedStatement related = con.prepareStatement
		(@sql.adminUpdate.related@);

	    // Set parameter
	    related.setInt(1, i_id);	
	    related.setInt(2, i_id);
	    ResultSet rs = related.executeQuery();
	    
	    int[] related_items = new int[5];
	    // Results
	    int counter = 0;
	    int last = 0;
	    while(rs.next()) {
		last = rs.getInt(1);
		related_items[counter] = last;
		counter++;
	    }

	    // This is the case for the situation where there are not 5 related books.
	    for (int i=counter; i<5; i++) {
		last++;
		related_items[i] = last;
	    }
	    rs.close();
	    related.close();

	    {
		// Prepare SQL
		statement = con.prepareStatement
		    (@sql.adminUpdate.related1@);
		
		// Set parameter
		statement.setInt(1, related_items[0]);
		statement.setInt(2, related_items[1]);
		statement.setInt(3, related_items[2]);
		statement.setInt(4, related_items[3]);
		statement.setInt(5, related_items[4]);
		statement.setInt(6, i_id);
		statement.executeUpdate();
	    }
	    statement.close();
	    
		try {
			Debug.println("Set shadow op for adminUpdate");
			DBTPCWShdAdminUpdate dAu = null;
			dAu = DBTPCWShdAdminUpdate.createOperation(i_id, cost, image, thumbnail, pubdate, related_items[0],
		    		related_items[1],related_items[2],related_items[3],related_items[4]);
			con.setShadowOperation(dAu, 0); 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static String GetUserName(final int C_ID){
	String string = withTransaction(new tx.TransactionalCommand<String>() {
		public String doIt(Connection con) throws SQLException {
		    return GetUserName((TxMudConnection)con, C_ID);
		}
	    });
	return string;
    }
    private static String GetUserName(TxMudConnection con, int C_ID)throws SQLException {
	String u_name = null;
	    PreparedStatement get_user_name = con.prepareStatement
		(@sql.getUserName@);
	    
	    // Set parameter
	    get_user_name.setInt(1, C_ID);
	    ResultSet rs = get_user_name.executeQuery();
	    
	    // Results
	    rs.next();
	    u_name = rs.getString("c_uname");
	    rs.close();

	    get_user_name.close();
	    try{
	    	Debug.println("Set empty shadow op for GetUserName");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return u_name;
    }

    public static String GetPassword(final String C_UNAME){
	String string = withTransaction(new tx.TransactionalCommand<String>() {
		public String doIt(Connection con) throws SQLException {
		    return GetPassword((TxMudConnection)con, C_UNAME);
		}
	    });
	return string;
    }
    private static String GetPassword(TxMudConnection con, String C_UNAME)throws SQLException {
	String passwd = null;
	    PreparedStatement get_passwd = con.prepareStatement
		(@sql.getPassword@);
	    
	    // Set parameter
	    get_passwd.setString(1, C_UNAME);
	    ResultSet rs = get_passwd.executeQuery();
	    
	    // Results
	    rs.next();
	    passwd = rs.getString("c_passwd");
	    rs.close();

	    get_passwd.close();
	    try{
	    	Debug.println("Set empty shadow op for GetPassword");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return passwd;
    }

    //This function gets the value of I_RELATED1 for the row of
    //the item table corresponding to I_ID
    private static int getRelated1(int I_ID, TxMudConnection con) throws SQLException {
	int related1 = -1;
	    PreparedStatement statement = con.prepareStatement
		(@sql.getRelated1@);
	    statement.setInt(1, I_ID);
	    ResultSet rs = statement.executeQuery();
	    rs.next();
	    related1 = rs.getInt(1);//Is 1 the correct index?
	    rs.close();
	    statement.close();
	return related1;
    }

    public static Order GetMostRecentOrder(final String c_uname, final Vector order_lines){
	Order order = withTransaction(new tx.TransactionalCommand<Order>() {
		public Order doIt(Connection con) throws SQLException {
		    return GetMostRecentOrder((TxMudConnection)con, c_uname, order_lines);
		}
	    });
	return order;
    }
    private static Order GetMostRecentOrder(TxMudConnection con, String c_uname, Vector order_lines)throws SQLException {
	    order_lines.removeAllElements();
	    int order_id;
	    Order order;

	    {
		// *** Get the o_id of the most recent order for this user
		PreparedStatement get_most_recent_order_id = con.prepareStatement
		    (@sql.getMostRecentOrder.id@);
		
		// Set parameter
		get_most_recent_order_id.setString(1, c_uname);
		ResultSet rs = get_most_recent_order_id.executeQuery();
		
		if (rs.next()) {
		    order_id = rs.getInt("o_id");
		} else {
		    // There is no most recent order
		    rs.close();
		    get_most_recent_order_id.close();
		    try{
		    	Debug.println("Set empty shadow op for GetMostRecentOrder null");
		    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
		    	con.setShadowOperation(dEm, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return null;
		}
		rs.close();
		get_most_recent_order_id.close();
	    }
	    
	    {
		// *** Get the order info for this o_id
		PreparedStatement get_order = con.prepareStatement
		    (@sql.getMostRecentOrder.order@);
		
		// Set parameter
		get_order.setInt(1, order_id);
		ResultSet rs2 = get_order.executeQuery();
		
		// Results
		if (!rs2.next()) {
		    // FIXME - This case is due to an error due to a database population error
		    rs2.close();
		    //		    get_order.close();
		    try{
		    	Debug.println("Set empty shadow op for GetMostRecentOrder null");
		    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
		    	con.setShadowOperation(dEm, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return null;
		}
		order = new Order(rs2);
		rs2.close();
		get_order.close();
	    }

	    {
		// *** Get the order_lines for this o_id
		PreparedStatement get_order_lines = con.prepareStatement
		    (@sql.getMostRecentOrder.lines@);
		
		// Set parameter
		get_order_lines.setInt(1, order_id);
		ResultSet rs3 = get_order_lines.executeQuery();
		
		// Results
		while(rs3.next()) {
		    order_lines.addElement(new OrderLine(rs3));
		}
		rs3.close();
		get_order_lines.close();
	    }
	    try{
	    	Debug.println("Set empty shadow op for GetMostRecentOrder not null");
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return order;
    }

    // ********************** Shopping Cart code below ************************* 

    // Called from: TPCW_shopping_cart_interaction 
    public static int createEmptyCart(){
	Integer integer = withTransaction(new tx.TransactionalCommand<Integer>() {
		public Integer doIt(Connection con) throws SQLException {
		    return createEmptyCart((TxMudConnection)con);
		}
	    });
	return integer;
    }
    private static int createEmptyCart(TxMudConnection con)throws SQLException {
	int SHOPPING_ID = -1;
	/*int coin = TPCW_Util.getRandom(10);
	if(coin == 1){
		//get an existing shopping cart
		System.err.println("coin 1");
		int highest_s_id = ShoppingIDFactory.get();
		double rnd = doubleGenerator.nextDouble();
		int tmp_id = (int)( highest_s_id * rnd);
		Debug.printf("tmp_id %d  s_id %d rand %f\n", tmp_id, highest_s_id, rnd);
		PreparedStatement check_cart = con.prepareStatement(@sql.checkCartExist@);
		check_cart.setInt(1, tmp_id);
		Debug.println(check_cart.toString());
	    ResultSet rs = check_cart.executeQuery();
	    if(rs.next()){
	    	Debug.printf("create empty cart cart %d exists s_id %d rand %f\n", tmp_id, highest_s_id, rnd);
	    	SHOPPING_ID = tmp_id;
	    	
	    	try{
	    		Debug.println("Set empty shadow op for createEmptyCart null");
	    		DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    		con.setShadowOperation(dEm, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    }else{
	    	Debug.printf("create empty cart cart %d doesn't exist s_id %d rand %f\n", tmp_id,highest_s_id, rnd);
	    }
	    rs.close();
	    check_cart.close();
	}else{
		Debug.println("coin 2");
	}
	if(SHOPPING_ID == -1){*/
		SHOPPING_ID = ShoppingIDFactory.addAndGet(totalproxies);
		PreparedStatement insert_cart = con.prepareStatement(@sql.createEmptyCart.txmud@);
		insert_cart.setInt(1, SHOPPING_ID);
		Calendar calendar = Calendar.getInstance();
		long sc_time = calendar.getTimeInMillis();
		insert_cart.setDate(2, new java.sql.Date(sc_time));
		insert_cart.executeUpdate();
		
		try{
			Debug.println("Set empty shadow op for createEmptyCart not null");
			DBTPCWShdCreateEmptyCart dCEm= DBTPCWShdCreateEmptyCart.createOperation(SHOPPING_ID, sc_time);
			con.setShadowOperation(dCEm, 0);//TODO: change to red
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	//}
	    
	return SHOPPING_ID;
    }
    
    public static Cart doCart(final int SHOPPING_ID, final Integer I_ID, final Vector ids, final Vector quantities) {	
	Cart cart = withTransaction(new tx.TransactionalCommand<Cart>() {
		public Cart doIt(Connection con) throws SQLException {
		    return doCart((TxMudConnection)con, SHOPPING_ID, I_ID, ids, quantities);
		}
	    });
	return cart;
    }
    private static Cart doCart(TxMudConnection con, int SHOPPING_ID, Integer I_ID, Vector ids, Vector quantities) throws SQLException {
	    Cart cart = null;
	    ShdDoCartData sDc = new ShdDoCartData(SHOPPING_ID);
	    
	    if (I_ID != null) {
		addItem(con, SHOPPING_ID, I_ID.intValue(), sDc); 
	    }
	    refreshCart(con, SHOPPING_ID, ids, quantities, sDc);
	    addRandomItemToCartIfNecessary(con, SHOPPING_ID, sDc);
	    resetCartTime(con, SHOPPING_ID, sDc);
	    cart = TPCW_Database.getCart(con, SHOPPING_ID, 0.0);
	    
	    //generate shadow operations
	    Debug.println("Set shadow op for doCart");
	    
	    if(sDc.isInserted()){
	    	if(sDc.isRemoved()){
	    		//insert and remove
	    		DBTPCWShdDoCart5 dDc5 = sDc.getDoCart5();
	    		con.setShadowOperation(dDc5, 0);
	    	}else{
	    		if(sDc.isUpdated()){
	    			//insert and update
	    			DBTPCWShdDoCart2 dDc2 = sDc.getDoCart2();
	    			con.setShadowOperation(dDc2, 0);
	    		}else{
	    			//only insertion
	    			DBTPCWShdDoCart1 dDc1 = sDc.getDoCart1();
	    			con.setShadowOperation(dDc1, 0);
	    		}
	    	}
	    }else{
	    	if(sDc.isUpdated()){
	    		if(sDc.isRemoved()){
	    			//update + remove
	    			DBTPCWShdDoCart4 dDc4 = sDc.getDoCart4();
	    			con.setShadowOperation(dDc4, 0);
	    		}else{
	    			//only update
	    			DBTPCWShdDoCart3 dDc3 = sDc.getDoCart3();
	    			con.setShadowOperation(dDc3, 0);
	    		}
	    	}else {
	    		if(sDc.isRemoved()) {
	    			//only remove
	    			DBTPCWShdDoCart7 dDc7 = sDc.getDoCart7();
	    			con.setShadowOperation(dDc7, 0);
	    		}else {
	    			//nothing, only update the access time
	    			DBTPCWShdDoCart6 dDc6 = sDc.getDoCart6();
	    			con.setShadowOperation(dDc6, 0);
	    		}
	    	}
	    }
	return cart;
    }

    //This function finds the shopping cart item associated with SHOPPING_ID
    //and I_ID. If the item does not already exist, we create one with QTY=1,
    //otherwise we increment the quantity.

    private static void addItem(TxMudConnection con, int SHOPPING_ID, int I_ID, ShdDoCartData sDc) throws SQLException {
    	PreparedStatement find_entry = con.prepareStatement
		(@sql.addItem@);
	    
	    // Set parameter
	    find_entry.setInt(1, SHOPPING_ID);
	    find_entry.setInt(2, I_ID);
	    ResultSet rs = find_entry.executeQuery();
	    
	    // Results
	    if(rs.next()) {
		//The shopping cart id, item pair were already in the table
		int currqty = rs.getInt("scl_qty");
		currqty+=1;
		PreparedStatement update_qty = con.prepareStatement
		(@sql.addItem.update@);
		update_qty.setInt(1, currqty);
		update_qty.setInt(2, SHOPPING_ID);
		update_qty.setInt(3, I_ID);
		update_qty.executeUpdate();
		update_qty.close();
		
		//set parameter
		sDc.setUpdateInfo(I_ID,currqty);
	    } else {//We need to add a new row to the table.
		
		//Stick the item info in a new shopping_cart_line
		PreparedStatement put_line = con.prepareStatement
		    (@sql.addItem.put@);
		put_line.setInt(1, SHOPPING_ID);
		put_line.setInt(2, 1);
		put_line.setInt(3, I_ID);
		put_line.executeUpdate();
		put_line.close();
		
		//set parameter
		sDc.setInsertInfo(I_ID);
	    }
	    rs.close();
	    find_entry.close();
    }

    private static void refreshCart(TxMudConnection con, int SHOPPING_ID, Vector ids, 
				    Vector quantities, ShdDoCartData sDc) throws SQLException {
	int i;
	    for(i = 0; i < ids.size(); i++){
		String I_IDstr = (String) ids.elementAt(i);
		String QTYstr = (String) quantities.elementAt(i);
		int I_ID = Integer.parseInt(I_IDstr);
		int QTY = Integer.parseInt(QTYstr);
		
		if(QTY == 0) { // We need to remove the item from the cart
		    PreparedStatement statement = con.prepareStatement
			(@sql.refreshCart.remove@);
		    statement.setInt(1, SHOPPING_ID);
		    statement.setInt(2, I_ID);
		    statement.executeUpdate();
		    statement.close();
		    //set parameter
		    sDc.setRemoveInfo(I_ID);
   		} 
		else { //we update the quantity
		    PreparedStatement statement = con.prepareStatement
			(@sql.refreshCart.update@);
		    statement.setInt(1, QTY);
		    statement.setInt(2, SHOPPING_ID);
		    statement.setInt(3, I_ID);
		    statement.executeUpdate(); 
		    statement.close();
		    sDc.setUpdateInfo(I_ID, QTY);
		}
	    }
    }

    private static void addRandomItemToCartIfNecessary(TxMudConnection con, int SHOPPING_ID, ShdDoCartData sDc) throws SQLException {
	// check and see if the cart is empty. If it's not, we do
	// nothing.
	int related_item = 0;
	
	    // Check to see if the cart is empty
	    PreparedStatement get_cart = con.prepareStatement
		(@sql.addRandomItemToCartIfNecessary@);
	    get_cart.setInt(1, SHOPPING_ID);
	    ResultSet rs = get_cart.executeQuery();
	    rs.next();
	    if (rs.getInt(1) == 0) {
		// Cart is empty
		int rand_id = TPCW_Util.getRandomI_ID();
		related_item = getRelated1(rand_id,con);
		addItem(con, SHOPPING_ID, related_item, sDc);
	    }
	    
	    rs.close();
	    get_cart.close();
    }


    // Only called from this class 
    private static void resetCartTime(TxMudConnection con, int SHOPPING_ID,ShdDoCartData sDc) throws SQLException {
	    PreparedStatement statement = con.prepareStatement
		(@sql.resetCartTime.txmud@);
	
	    // Set parameter
	    Calendar calendar = Calendar.getInstance();
	    long access_time = calendar.getTimeInMillis();
	    statement.setDate(1, new java.sql.Date(access_time));
	    statement.setInt(2, SHOPPING_ID);
	    statement.executeUpdate();
	    statement.close();
	    sDc.setAccessTime(access_time);
    }

    public static Cart getCart(final int SHOPPING_ID, final double c_discount) {
	Cart cart = withTransaction(new tx.TransactionalCommand<Cart>() {
		public Cart doIt(Connection con) throws SQLException {
		    Cart cart = getCart((TxMudConnection)con, SHOPPING_ID, c_discount);
		    try{
		    	Debug.println("Set empty shadow op for GetCart");
		    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
		    	((TxMudConnection)con).setShadowOperation(dEm, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return cart;
		}
	    });
	return cart;
    }
    //time .05s
    private static Cart getCart(TxMudConnection con, int SHOPPING_ID, double c_discount) throws SQLException {
	Cart mycart = null;
	    PreparedStatement get_cart = con.prepareStatement
		(@sql.getCart@);
	    get_cart.setInt(1, SHOPPING_ID);
	    ResultSet rs = get_cart.executeQuery();
	    mycart = new Cart(rs, c_discount);
	    rs.close();
	    get_cart.close();
	return mycart;
    }

    // ************** Customer / Order code below ************************* 

    //This should probably return an error code if the customer
    //doesn't exist, but ...
    public static void refreshSession(final int C_ID) {
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    refreshSession((TxMudConnection)con, C_ID);
		    return VOID;
		}
	    });
    }
    private static void refreshSession(TxMudConnection con, int C_ID) throws SQLException {
	    PreparedStatement updateLogin = con.prepareStatement
		(@sql.refreshSession.txmud@);
	    
	    // Set parameter
	    Calendar calendar = Calendar.getInstance();
	    long loginTs = calendar.getTimeInMillis();
	    calendar.add(Calendar.HOUR,2);
	    long expireTs = calendar.getTimeInMillis();
	    
	    updateLogin.setDate(1, new java.sql.Date(loginTs));
	    updateLogin.setDate(2, new java.sql.Date(expireTs));
	    updateLogin.setInt(3, C_ID);
	    updateLogin.executeUpdate();
	    
	    updateLogin.close();
	    
	    try{
	    	DBTPCWShdRefreshSession dRs = DBTPCWShdRefreshSession.createOperation(C_ID, loginTs, expireTs);
	    	con.setShadowOperation(dRs, 0);//TODO: change to red
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
    }    

    public static Customer createNewCustomer(final Customer cust) {
	Customer customer = withTransaction(new tx.TransactionalCommand<Customer>() {
		public Customer doIt(Connection con) throws SQLException {
		    return createNewCustomer((TxMudConnection)con, cust);
		}
	    });
	return customer;
    }
    private static Customer createNewCustomer(TxMudConnection con, Customer cust) throws SQLException {
	    // Get largest customer ID already in use.
	    Debug.println("create new customer now");
	    cust.c_discount = (int) (java.lang.Math.random() * 51);
	    cust.c_balance =0.0;
	    cust.c_ytd_pmt = 0.0;
	    // FIXME - Use SQL CURRENT_TIME to do this
	    cust.c_last_visit = new Date(System.currentTimeMillis());
	    cust.c_since = new Date(System.currentTimeMillis());
	    cust.c_login = new Date(System.currentTimeMillis());
	    cust.c_expiration = new Date(System.currentTimeMillis() + 
					 7200000);//milliseconds in 2 hours
	    PreparedStatement insert_customer_row = con.prepareStatement
		(@sql.createNewCustomer@);
	    insert_customer_row.setString(4,cust.c_fname);
	    insert_customer_row.setString(5,cust.c_lname);
	    insert_customer_row.setString(7,cust.c_phone);
	    insert_customer_row.setString(8,cust.c_email);
	    insert_customer_row.setDate(9, new java.sql.Date(cust.c_since.getTime()));
	    insert_customer_row.setDate(10, new java.sql.Date(cust.c_last_visit.getTime()));
	    insert_customer_row.setDate(11, new java.sql.Date(cust.c_login.getTime()));
	    insert_customer_row.setDate(12, new java.sql.Date(cust.c_expiration.getTime()));
	    insert_customer_row.setDouble(13, cust.c_discount);
	    insert_customer_row.setDouble(14, cust.c_balance);
	    insert_customer_row.setDouble(15, cust.c_ytd_pmt);
	    insert_customer_row.setDate(16, new java.sql.Date(cust.c_birthdate.getTime()));
	    insert_customer_row.setString(17, cust.c_data);
	
	    boolean[] newAddr = new boolean[1];
	    newAddr[0] = false;
	    int[] country_id = new int[1];
	    country_id[0] = 0;
	    cust.addr_id = enterAddress(con, 
					cust.addr_street1, 
					cust.addr_street2,
					cust.addr_city,
					cust.addr_state,
					cust.addr_zip,
					cust.co_name, newAddr, country_id);
	    //get a unique ID 
	    cust.c_id=CustomerIDFactory.addAndGet(totalproxies);
	    		
		cust.c_uname = TPCW_Util.DigSyl(cust.c_id, 0);
		cust.c_passwd = cust.c_uname.toLowerCase();

		
		insert_customer_row.setInt(1, cust.c_id);
		insert_customer_row.setString(2,cust.c_uname);
		insert_customer_row.setString(3,cust.c_passwd);
		insert_customer_row.setInt(6, cust.addr_id);
		insert_customer_row.executeUpdate();
		insert_customer_row.close();
		
		/*String addrStr = cust.addr_id + "-" + cust.addr_street1 + "-" + cust.addr_street2;
		addrStr += "-" + cust.addr_city + "-" + cust.addr_state + "-" + cust.addr_zip;
		
		System.out.println("address input " + addrStr);*/
		
		//generate shadow operation
		if(newAddr[0] == false){
			//System.out.println("no new address");
			try{
				DBTPCWShdCreateNewCustomer2 dCnc = DBTPCWShdCreateNewCustomer2.createOperation(cust.c_id,
					cust.c_uname, cust.c_passwd, cust.c_discount,cust.c_fname, cust.c_lname,cust.c_phone, cust.c_email,
					cust.c_last_visit.getTime(), cust.c_since.getTime(), cust.c_login.getTime(), cust.c_expiration.getTime(),
					cust.c_birthdate.getTime(), cust.c_data, cust.addr_id);
				con.setShadowOperation(dCnc, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			//System.out.println("have a new address");
			try{
				DBTPCWShdCreateNewCustomer1 dCnc = DBTPCWShdCreateNewCustomer1.createOperation(cust.c_id,
					cust.c_uname, cust.c_passwd, cust.c_discount,cust.c_fname, cust.c_lname,cust.c_phone, cust.c_email,
					cust.c_last_visit.getTime(), cust.c_since.getTime(), cust.c_login.getTime(), cust.c_expiration.getTime(),
					cust.c_birthdate.getTime(), cust.c_data, cust.addr_id, cust.addr_street1, cust.addr_street2, cust.addr_city,
					cust.addr_state, cust.addr_zip, country_id[0]);
				con.setShadowOperation(dCnc, 0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	    
	return cust;
    }

    //BUY CONFIRM 

    public static BuyConfirmResult doBuyConfirm(final int shopping_id,
						final int customer_id,
						final String cc_type,
						final long cc_number,
						final String cc_name,
						final Date cc_expiry,
						final String shipping) {
	BuyConfirmResult buyConfirmResult = withTransaction(new tx.TransactionalCommand<BuyConfirmResult>() {
		public BuyConfirmResult doIt(Connection con) throws SQLException {
		    return doBuyConfirm((TxMudConnection)con, shopping_id, customer_id, cc_type, cc_number, cc_name, cc_expiry, shipping);
		}
	    });
	return buyConfirmResult;
    }
    private static BuyConfirmResult doBuyConfirm(TxMudConnection con, int shopping_id, int customer_id, String cc_type, long cc_number,
						 String cc_name, Date cc_expiry, String shipping) throws SQLException {
	
    	Debug.println("doBuyConfirm no address");
    	
	BuyConfirmResult result = new BuyConfirmResult();
	
		ShdDoBuyConfirmData sCd = new ShdDoBuyConfirmData();
	
	    double c_discount = getCDiscount(con, customer_id);
	    result.cart = getCart(con, shopping_id, c_discount);
	    int ship_addr_id = getCAddr(con, customer_id);
	    result.order_id = enterOrder(con, customer_id, result.cart, ship_addr_id, shipping, c_discount, sCd);
	    enterCCXact(con, result.order_id, cc_type, cc_number, cc_name, cc_expiry, result.cart.SC_TOTAL, ship_addr_id, sCd);
	    clearCart(con, shopping_id);
	    
	    //set parameters
	    sCd.setShoppingCartInfo(shopping_id, customer_id, c_discount);
	    sCd.setShipAddrId(ship_addr_id);
	    
		//set shadow operation
	    if(!sCd.isCartEmpty()){
			if(sCd.isIncreased()){
				DBTPCWShdDoBuyConfirm3 dBc = sCd.getShdDoBC3();
				con.setShadowOperation(dBc, 0);
			}else{
				DBTPCWShdDoBuyConfirm4 dBc = sCd.getShdDoBC4();
				con.setShadowOperation(dBc, 1);
			}
	    }else{
	    	if(sCd.isNewAddr()){
	    		DBTPCWShdDoBuyConfirm5 dBc = sCd.getShdDoBC5();
	    		con.setShadowOperation(dBc, 0);
	    	}else{
	    		DBTPCWShdDoBuyConfirm6 dBc = sCd.getShdDoBC6();
	    		con.setShadowOperation(dBc, 0);
	    	}
	    }
	    
	return result;
    }
    
    public static BuyConfirmResult doBuyConfirm(final int shopping_id,
						final int customer_id,
						final String cc_type,
						final long cc_number,
						final String cc_name,
						final Date cc_expiry,
						final String shipping,
						final String street_1, final String street_2,
						final String city, final String state,
						final String zip, final String country) {
	BuyConfirmResult buyConfirmResult = withTransaction(new tx.TransactionalCommand<BuyConfirmResult>() {
		public BuyConfirmResult doIt(Connection con) throws SQLException {
		    return doBuyConfirm((TxMudConnection)con, shopping_id, customer_id, cc_type, cc_number, cc_name, cc_expiry, shipping,
					street_1, street_2, city, state, zip, country);
		}
	    });
	return buyConfirmResult;
						}
    private static BuyConfirmResult doBuyConfirm(TxMudConnection con, int shopping_id, int customer_id, String cc_type, long cc_number,
						 String cc_name, Date cc_expiry, String shipping, String street_1, String street_2,
						 String city, String state, String zip, String country) throws SQLException {
    	
    	Debug.println("doBuyConfirm address");
    	ShdDoBuyConfirmData sCd = new ShdDoBuyConfirmData();
    	
    	BuyConfirmResult result = new BuyConfirmResult();
	    double c_discount = getCDiscount(con, customer_id);
	    result.cart = getCart(con, shopping_id, c_discount);
	    boolean[] newAddr = new boolean[1];
	    newAddr[0] = false;
	    int[] country_id = new int[1];
	    country_id[0] = 0;
	    int ship_addr_id = enterAddress(con, street_1, street_2, city, state, zip, country, newAddr, country_id);
	    if(newAddr[0] == true){
	    	sCd.setAddrInfo(street_1, street_2, city, state, zip, country_id[0]);
	    }
	    result.order_id = enterOrder(con, customer_id, result.cart, ship_addr_id, shipping, c_discount, sCd);
	    enterCCXact(con, result.order_id, cc_type, cc_number, cc_name, cc_expiry, result.cart.SC_TOTAL, ship_addr_id, sCd);
	    clearCart(con, shopping_id);
	    
	  //set parameters
	    sCd.setShoppingCartInfo(shopping_id, customer_id, c_discount);
	    sCd.setShipAddrId(ship_addr_id);
	    
	    
	    //generate shadow operation
	    Debug.println("Set shadow operation for doBuyConfirm address");
	    if(!sCd.isCartEmpty()){
		    if(sCd.isIncreased()){
		    	if(sCd.isNewAddr()){
		    		DBTPCWShdDoBuyConfirm1 dBc = sCd.getShdDoBC1();
			    	con.setShadowOperation(dBc, 0);
		    	}else{
		    		DBTPCWShdDoBuyConfirm3 dBc = sCd.getShdDoBC3();
			    	con.setShadowOperation(dBc, 0);
		    	}
		    }else{
		    	if(sCd.isNewAddr()){
		    		DBTPCWShdDoBuyConfirm2 dBc = sCd.getShdDoBC2();
			    	con.setShadowOperation(dBc, 1);
		    	}else{
		    		DBTPCWShdDoBuyConfirm4 dBc = sCd.getShdDoBC4();
			    	con.setShadowOperation(dBc, 1);
		    	}
		    }
	    }else{
	    	if(sCd.isNewAddr()){
	    		DBTPCWShdDoBuyConfirm5 dBc = sCd.getShdDoBC5();
	    		con.setShadowOperation(dBc, 0);
	    	}else{
	    		DBTPCWShdDoBuyConfirm6 dBc = sCd.getShdDoBC6();
	    		con.setShadowOperation(dBc, 0);
	    	}
	    }
	    
	    return result;
    }


    //DB query time: .05s
    public static double getCDiscount(TxMudConnection con, int c_id) throws SQLException {
	double c_discount = 0.0;
	    // Prepare SQL
	    PreparedStatement statement = con.prepareStatement
		(@sql.getCDiscount@);
	    
	    // Set parameter
	    statement.setInt(1, c_id);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    rs.next();
	    c_discount = rs.getDouble(1);
	    rs.close();
	    statement.close();
	return c_discount;
    }

    //DB time: .05s
    public static int getCAddrID(TxMudConnection con, int c_id) throws SQLException {
	int c_addr_id = 0;
	    // Prepare SQL
	    PreparedStatement statement = con.prepareStatement
		(@sql.getCAddrId@);
	    
	    // Set parameter
	    statement.setInt(1, c_id);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    rs.next();
	    c_addr_id = rs.getInt(1);
	    rs.close();
	    statement.close();
	return c_addr_id;
    }

    public static int getCAddr(TxMudConnection con, int c_id) throws SQLException {
	int c_addr_id = 0;
	    // Prepare SQL
	    PreparedStatement statement = con.prepareStatement
		(@sql.getCAddr@);
	    
	    // Set parameter
	    statement.setInt(1, c_id);
	    ResultSet rs = statement.executeQuery();
	    
	    // Results
	    rs.next();
	    c_addr_id = rs.getInt(1);
	    rs.close();
	    statement.close();
	return c_addr_id;
    }

    public static void enterCCXact(TxMudConnection con,
				   int o_id,        // Order id
				   String cc_type,
				   long cc_number,
				   String cc_name,
				   Date cc_expiry,
				   double total,   // Total from shopping cart
				   int ship_addr_id, ShdDoBuyConfirmData sCd) throws SQLException {

	// Updates the CC_XACTS table
	if(cc_type.length() > 10)
	    cc_type = cc_type.substring(0,10);
	if(cc_name.length() > 30)
	    cc_name = cc_name.substring(0,30);
	
	    // Prepare SQL
	    PreparedStatement statement = con.prepareStatement
		(@sql.enterCCXact.txmud@);
	    
	    // Set parameter
	    statement.setInt(1, o_id);           // cx_o_id
	    statement.setString(2, cc_type);     // cx_type
	    statement.setLong(3, cc_number);     // cx_num
	    statement.setString(4, cc_name);     // cx_name
	    statement.setDate(5, cc_expiry);     // cx_expiry
	    statement.setDouble(6, total);       // cx_xact_amount
	    long cc_pay_date = System.currentTimeMillis();
	    statement.setDate(7, new java.sql.Date(cc_pay_date));     // cx_xact_date
	    statement.setInt(8, ship_addr_id);   // ship_addr_id
	    statement.executeUpdate();
	    statement.close();
	    sCd.setCreditCartInfo(cc_type, cc_number, cc_name, 
	    		cc_expiry.getTime(), cc_pay_date);
    }
    
    public static void clearCart(TxMudConnection con, int shopping_id) throws SQLException {
	// Empties all the lines from the shopping_cart_line for the
	// shopping id.  Does not remove the actually shopping cart
	    // Prepare SQL
	    PreparedStatement statement = con.prepareStatement
		(@sql.clearCart@);
	    
	    // Set parameter
	    statement.setInt(1, shopping_id);
	    statement.executeUpdate();
	    statement.close();
    	
    }

    public static int enterAddress(TxMudConnection con,  // Do we need to do this as part of a transaction?
				   String street1, String street2,
				   String city, String state,
				   String zip, String country, boolean[] newAddr, int[] country_id) throws SQLException {
	// returns the address id of the specified address.  Adds a
	// new address to the table if needed
	int addr_id = 0;

        // Get the country ID from the country table matching this address.

        // Is it safe to assume that the country that we are looking
        // for will be there?
	    PreparedStatement get_co_id = con.prepareStatement
		(@sql.enterAddress.id@);
	    get_co_id.setString(1, country);
	    ResultSet rs = get_co_id.executeQuery();
	    rs.next();
	    int addr_co_id = rs.getInt("co_id");
	    rs.close();
	    get_co_id.close();
	    
	    country_id[0] = addr_co_id;
	    
	    //Get address id for this customer, possible insert row in
	    //address table
	    PreparedStatement match_address = con.prepareStatement
		(@sql.enterAddress.match@);
	    match_address.setString(1, street1);
	    match_address.setString(2, street2);
	    match_address.setString(3, city);
	    match_address.setString(4, state);
	    match_address.setString(5, zip);
	    match_address.setInt(6, addr_co_id);
	    rs = match_address.executeQuery();
	    if(!rs.next()){//We didn't match an address in the addr table
		PreparedStatement insert_address_row = con.prepareStatement
		    (@sql.enterAddress.insert@);
		insert_address_row.setString(2, street1);
		insert_address_row.setString(3, street2);
		insert_address_row.setString(4, city);
		insert_address_row.setString(5, state);
		insert_address_row.setString(6, zip);
		insert_address_row.setInt(7, addr_co_id);

	    addr_id = AddrIDFactory.addAndGet(totalproxies);
	    //Need to insert a new row in the address table
	    insert_address_row.setInt(1, addr_id);
	    insert_address_row.executeUpdate();

		insert_address_row.close();
		newAddr[0] = true;
	    } else { //We actually matched
		addr_id = rs.getInt("addr_id");
	    }
	    match_address.close();
	    rs.close();
	return addr_id;
    }

 
    public static int enterOrder(TxMudConnection con, int customer_id, Cart cart, int ship_addr_id, String shipping, double c_discount, ShdDoBuyConfirmData sCd) throws SQLException {
	// returns the new order_id
	int o_id = 0;
	Calendar calendar = Calendar.getInstance();
	// - Creates an entry in the 'orders' table 
	    PreparedStatement insert_row = con.prepareStatement
		(@sql.enterOrder.txmud@);
	    insert_row.setInt(2, customer_id);
	    long o_date = calendar.getTimeInMillis();
	    insert_row.setDate(3, new java.sql.Date(o_date));
	    insert_row.setDouble(4, cart.SC_SUB_TOTAL);
	    insert_row.setDouble(5, cart.SC_TOTAL);
	    insert_row.setString(6, shipping);
	    calendar.add(Calendar.DAY_OF_YEAR, TPCW_Util.getRandom(7));
	    long o_ship_date = calendar.getTimeInMillis();
	    insert_row.setDate(7, new java.sql.Date(o_ship_date));
	    int customer_addr_id = getCAddrID(con, customer_id);
	    insert_row.setInt(8, customer_addr_id);
	    insert_row.setInt(9, ship_addr_id);

		o_id = OrderLineIDFactory.addAndGet(totalproxies);
		insert_row.setInt(1, o_id);
		insert_row.executeUpdate();
	    insert_row.close();
	    
	    Debug.println("set order info");
	    //set parameters
	    sCd.setOrderInfo(o_id, o_date, cart.SC_SUB_TOTAL, cart.SC_TOTAL,
				shipping, o_ship_date, customer_addr_id);
	    
	    Debug.println("try to set order line info");
		//orderline
		Vector<Integer> it_id_v = new Vector<Integer>();
		Vector<Integer> it_q_v = new Vector<Integer>();
		Vector<String> it_c_v = new Vector<String>();//comment
		
		//modify stock
		Vector<Integer> it_s_v = new Vector<Integer>(); 
		//stock delta, should be positive

	Enumeration e = cart.lines.elements();
	int counter = 0;
	while(e.hasMoreElements()) {
	    // - Creates one or more 'order_line' rows.
	    CartLine cart_line = (CartLine) e.nextElement();
	    String orderComment = TPCW_Util.getRandomString(20, 100);
	    addOrderLine(con, counter, o_id, cart_line.scl_i_id, 
			 cart_line.scl_qty, c_discount, 
			 orderComment );
	    counter++;
	    
	    //set parameters
	    it_id_v.add(cart_line.scl_i_id);
	    it_q_v.add(cart_line.scl_qty);
	    it_c_v.add(orderComment);

	    // - Adjusts the stock for each item ordered
	    int stock = getStock(con, cart_line.scl_i_id);
	    if ((stock - cart_line.scl_qty) < 10) {
		setStock(con, cart_line.scl_i_id, 
			 stock - cart_line.scl_qty + 21);
			it_s_v.add(-cart_line.scl_qty + 21);
	    } else {
		setStock(con, cart_line.scl_i_id, stock - cart_line.scl_qty);
		it_s_v.add(-cart_line.scl_qty);
	    }
	}
	
	//set parameters
	sCd.setVectorInfo(it_id_v, it_q_v, it_c_v, it_s_v);
	
	return o_id;
    }
    
    public static void addOrderLine(TxMudConnection con, 
				    int ol_id, int ol_o_id, int ol_i_id, 
				    int ol_qty, double ol_discount, String ol_comment) throws SQLException {
	int success = 0;
	    PreparedStatement insert_row = con.prepareStatement
		(@sql.addOrderLine@);
	    
	    insert_row.setInt(1, ol_id);
	    insert_row.setInt(2, ol_o_id);
	    insert_row.setInt(3, ol_i_id);
	    insert_row.setInt(4, ol_qty);
	    insert_row.setDouble(5, ol_discount);
	    insert_row.setString(6, ol_comment);
	    insert_row.executeUpdate();
	    insert_row.close();
    }

    public static int getStock(TxMudConnection con, int i_id) throws SQLException {
	int stock = 0;
	    PreparedStatement get_stock = con.prepareStatement
		(@sql.getStock@);
	    
	    // Set parameter
	    get_stock.setInt(1, i_id);
	    ResultSet rs = get_stock.executeQuery();
	    
	    // Results
	    rs.next();
	    stock = rs.getInt("i_stock");
	    rs.close();
	    get_stock.close();
	return stock;
    }

    public static void setStock(TxMudConnection con, int i_id, int new_stock) throws SQLException {
	    PreparedStatement update_row = con.prepareStatement
		(@sql.setStock@);
	    update_row.setInt(1, new_stock);
	    update_row.setInt(2, i_id);
	    update_row.executeUpdate();
	    update_row.close();
    }

    public static void verifyDBConsistency(){
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    verifyDBConsistency((TxMudConnection)con);
		    return VOID;
		}
	    });
    }
    private static void verifyDBConsistency(TxMudConnection con) throws SQLException {
	    int this_id;
	    int id_expected = 1;
	    //First verify customer table
	    PreparedStatement get_ids = con.prepareStatement
		(@sql.verifyDBConsistency.custId@);
	    ResultSet rs = get_ids.executeQuery();
	    while(rs.next()){
	        this_id = rs.getInt("c_id");
		while(this_id != id_expected){
		    System.out.println("Missing C_ID " + id_expected);
		    id_expected++;
		}
		id_expected++;
	    }
	    
	    id_expected = 1;
	    //Verify the item table
	    get_ids = con.prepareStatement
		(@sql.verifyDBConsistency.itemId@);
	    rs = get_ids.executeQuery();
	    while(rs.next()){
	        this_id = rs.getInt("i_id");
		while(this_id != id_expected){
		    System.out.println("Missing I_ID " + id_expected);
		    id_expected++;
		}
		id_expected++;
	    }

	    id_expected = 1;
	    //Verify the address table
	    get_ids = con.prepareStatement
		(@sql.verifyDBConsistency.addrId@);
	    rs = get_ids.executeQuery();
	    while(rs.next()){
	        this_id = rs.getInt("addr_id");
		//		System.out.println(this_cid+"\n");
		while(this_id != id_expected){
		    System.out.println("Missing ADDR_ID " + id_expected);
		    id_expected++;
		}
		id_expected++;
	    }
    }
    public synchronized static void initDatabasePool(){
    	if(use_connection_pool==false || pool_initialized==true)
    		return;

    	System.err.println("initialize txmud connection pool");
    	Vector<TxMudConnection> connections = new Vector<TxMudConnection>();
    	TxMudDriver.proxy = proxy.imp;
    	int i=0;
    	try{
	    	for(i=0;i<maxConn-availConn.size();i++){
	    		connections.add(getConnection());
	    		//transactions--;
	    	}
	    	for(i=0;i<connections.size();i++){
	    		returnConnection(connections.get(i));
	    	}
    	}catch(Exception e){
    		System.err.println("Problem when initializing database pool. connection:"+(i+1));
    		System.exit(0);
    	}
    	pool_initialized=true;
    	System.err.println("Total avaiable connections:"+availConn.size());
    	
    }
    public synchronized static void initID(int globalProxyId){
    	TxMudConnection con = getConnection();
    	int n;
    	try{
    	//PreparedStatement get_next_id = con.prepareStatement(@sql.createEmptyCart@);
    		Statement get_next_id = con.createStatement();
    		
    	ResultSet rs = get_next_id.executeQuery(@sql.createEmptyCart@);
		try{
	    	if(rs.next()){
				n = rs.getInt(1);
			}else{
				n=0;
			}
		}catch(NumberFormatException ne){
			n=0;
			System.err.println("Exception caught while initializing the Shopping id." +
					" Hopefuly the it is just because the table is empty.");
		}
		//n= n - (n % totalproxies) + totalproxies + globalProxyId;
		globalProxyId = proxy.getMyGlobalProxyId();
		System.err.println("my globalProxyId: " + globalProxyId + " totalproxy " + totalproxies);
		n = n+globalProxyId;
		System.err.println("Proxy["+proxyId+"] set initial Shopping ID:"+n);
		ShoppingIDFactory.set(n);
		rs.close();
    	/////////////////
	
		PreparedStatement get_max_id = con.prepareStatement(@sql.enterOrder.maxId@);
		ResultSet rs2 = get_max_id.executeQuery();
		try{
			if(rs2.next()){
				n = rs2.getInt(1);
			}else{
				n=0;
			}
		}catch(NumberFormatException ne){
			n=0;
			System.err.println("Exception caught while initializing the Order id." +
					" Hopefuly the it is just because the table is empty.");
		}
		//n= n - (n % totalproxies) + totalproxies + globalProxyId;
		n = n + globalProxyId;
		System.err.println("Proxy["+proxyId+"] set initial Order ID:"+n);
		OrderLineIDFactory.set(n);
		rs2.close();
    	//////////
		PreparedStatement get_max_addr_id = con.prepareStatement(@sql.enterAddress.maxId@);
    	ResultSet rs3 = get_max_addr_id.executeQuery();
    	try{
	    	if(rs3.next()){
				n = rs3.getInt(1);
			}else{
				n=0;
			}
    	}catch(NumberFormatException ne){
			n=0;
			System.err.println("Exception caught while initializing the Address id." +
					" Hopefuly the it is just because the table is empty.");
		}
    	//n= n - (n % totalproxies) + totalproxies + globalProxyId;
    	n = n + globalProxyId;
    	System.err.println("Proxy["+proxyId+"] set initial Addr ID:"+n);
		AddrIDFactory.set(n);
		rs3.close();
	    ///////

	    PreparedStatement get_max_customer_id = con.prepareStatement(@sql.createNewCustomer.maxId@);
		ResultSet rs_customer = get_max_customer_id .executeQuery();
		try{
	    	if(rs_customer .next()){
				n = rs_customer .getInt(1);
			}else{
				n=0;
			}
    	}catch(NumberFormatException ne){
			n=0;
			System.err.println("Exception caught while initializing the Customer id." +
					" Hopefuly the it is just because the table is empty.");
		}
		//n= n - (n % totalproxies) + totalproxies + globalProxyId;
    	n = n + globalProxyId;
    	System.err.println("Proxy["+proxyId+"] set initial Customer ID:"+n);
		CustomerIDFactory.set(n);
		try{
	    	DBTPCWShdEmpty dEm = DBTPCWShdEmpty.createOperation();
	    	con.setShadowOperation(dEm, 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		con.commit();
    	returnConnection(con);
    	}catch(Exception e){
    		System.err.println("Problem when setting the inital id, system cannot continue");
    		e.printStackTrace();
    		System.exit(0);
    	}
    	
    	
    	
    }//method
}



class TPCW_TxMud_Proxy implements ApplicationInterface {
	String proxy_cnf=""; 
	int dcId=0;
	int proxyId=0;
	int proxyThreads=10; 
	boolean tcpnodelay=false ;
	ClosedLoopProxy imp; 
	NettyTCPSender sendNet;
	ParallelPassThroughNetworkQueue ptnq;
	NettyTCPReceiver rcv;

	public TPCW_TxMud_Proxy(int dcid, int proxyid, int threads, String file, int c, int ssId, String dbXmlFile, int s){
		this.proxy_cnf=file; 
		this.dcId=dcid;
		this.proxyId=proxyid;
		this.proxyThreads=threads;
		Debug.println("proxy initializing");
		this.imp = new ClosedLoopProxy(proxy_cnf, dcId, proxyId, this,new ExecuteScratchpadFactory(c, dcid, 0, dbXmlFile,s));
		Debug.println("proxy initialized");
		// set up the networking channels
		//sender
		sendNet = new NettyTCPSender();
		imp.setSender(sendNet);
		sendNet.setTCPNoDelay(tcpnodelay);
		sendNet.setKeepAlive(true);
		//receiver
		ptnq = new ParallelPassThroughNetworkQueue(imp, proxyThreads);
		rcv = new NettyTCPReceiver(imp.getMembership().getMe().getInetSocketAddress(), ptnq, proxyThreads);
	}
	
	public int getMyGlobalProxyId(){
		if(this.dcId == 0)
			return this.proxyId;
		int dcCount = this.imp.getDatacenterCount();
		int globalProxyId = 0;
		for(int i = 0 ; i < dcCount; i++){
			if(i== this.dcId)
				break;
			else{
				globalProxyId += this.imp.getMembership().getProxyCount(i);
			}
		}
		globalProxyId = globalProxyId + this.proxyId;
		return globalProxyId;
	}
	public int selectStorageServer(Operation op) {
		return 0;
	}

	public int selectStorageServer(byte[] op) {
		return 0;
	}

	public int getBlueTransactions(){
		System.err.println("Blue transactions:"+imp.bluetnxcounter.get());
		return imp.bluetnxcounter.get();
		
	}
	public int getRedTransactions(){
		System.err.println("Red transactions:"+imp.redtnxcounter.get());
		return imp.redtnxcounter.get();
	}
	public int getAbortedTransactions(){
		System.err.println("Aborted transactions:"+imp.aborttnxcounter.get());
		return imp.aborttnxcounter.get();
	}
	public void setMeasurementInterval(long startmi, long endmi){
		System.err.println("Set measurement Interval:"+new Date(startmi) +" to "+new Date(endmi));
		imp.startmi=startmi;
		imp.endmi=endmi;
		imp.bluetnxcounter.set(0);
		imp.redtnxcounter.set(0);
		imp.aborttnxcounter.set(0);

	}
}

