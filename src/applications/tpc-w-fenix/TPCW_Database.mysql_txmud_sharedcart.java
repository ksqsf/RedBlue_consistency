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
import java.sql.SQLException;
import java.sql.Timestamp;


//scratchpad
import txstore.proxy.ClosedLoopProxy;
import txstore.scratchpad.ScratchpadConfig;
import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.DBScratchpad;
import txstore.scratchpad.rdbms.jdbc.PassThroughProxy;
import txstore.scratchpad.rdbms.jdbc.TxMudDriver;
import txstore.scratchpad.rdbms.resolution.AllOpsLockExecution;
import txstore.scratchpad.rdbms.jdbc.TxMudConnection;
import txstore.scratchpad.rdbms.util.tpcw.*;

//txmud
import network.ParallelPassThroughNetworkQueue;
import network.netty.NettyTCPReceiver;
import network.netty.NettyTCPSender;
import txstore.proxy.ApplicationInterface;
import txstore.proxy.ClosedLoopProxy;
import txstore.util.Operation;
import txstore.scratchpad.rdbms.util.tpcw.*;

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
    static int globalProxyId=0;
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
    
    // Here's what the db line looks like for postgres
    //public static final String url = "jdbc:postgresql://eli.ece.wisc.edu/tpcwb";

    
    // Get a connection from the pool.
    public static synchronized TxMudConnection getConnection() {
    	//count only transactions within measurement interval
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
    public static synchronized void returnConnection(Connection con)
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
    	System.err.println("INFO: creating new connection");
		System.err.println("INFO: configuring txmud: " +
				"actualDriver <"+actualDriver+"> jdbcActualpath <jdbcActualPath>"+
				"username:<"+user+"> password:<"+password+"> padClass: <"+padClass+">");
	try {
	    Class.forName(driver);
	    // Class.forName("postgresql.Driver");

	    // Create URL for specifying a DBMS
	    TxMudConnection con;
	    while(true) {
		try {
		    //   con = DriverManager.getConnection("jdbc:postgresql://eli.ece.wisc.edu/tpcw", "milo", "");
		    con = (TxMudConnection) DriverManager.getConnection(jdbcPath);
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
		Connection con = getConnection();
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
		    return getName(con, c_id);
		}
	    });
	return name;
    }
    private static String[] getName(Connection con, int c_id) throws SQLException {
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
	return name;
    }

    public static Book getBook(final int i_id) {
	Book book = withTransaction(new tx.TransactionalCommand<Book>() {
		public Book doIt(Connection con) throws SQLException {
		    return getBook(con, i_id);
		}
	    });
	return book;
    }
    private static Book getBook(Connection con, int i_id) throws SQLException {
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
	return book;
    }

    public static Customer getCustomer(final String UNAME){
	Customer customer = withTransaction(new tx.TransactionalCommand<Customer>() {
		public Customer doIt(Connection con) throws SQLException {
		    return getCustomer(con, UNAME);
		}
	    });
	return customer;
    }
    private static Customer getCustomer(Connection con, String UNAME) throws SQLException {
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
		return null;
	    }
	    
	    statement.close();
	return cust;
    }

    public static Vector doSubjectSearch(final String search_key) {
	Vector Vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doSubjectSearch(con, search_key);
		}
	    });
	return Vector;
    }
    private static Vector doSubjectSearch(Connection con, String search_key) throws SQLException {
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
	return vec;	
    }

    public static Vector doTitleSearch(final String search_key) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doTitleSearch(con, search_key);
		}
	    });
	return vector;
    }
    private static Vector doTitleSearch(Connection con, String search_key) throws SQLException {
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
	return vec;	
    }

    public static Vector doAuthorSearch(final String search_key) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return doAuthorSearch(con, search_key);
		}
	    });
	return vector;
    }
    private static Vector doAuthorSearch(Connection con, String search_key) throws SQLException {
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
	return vec;	
    }

    public static Vector getNewProducts(final String subject) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return getNewProducts(con, subject);
		}
	    });
	return vector;
    }
    private static Vector getNewProducts(Connection con, String subject) throws SQLException {
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
	return vec;	
    }

    public static Vector getBestSellers(final String subject) {
	Vector vector = withTransaction(new tx.TransactionalCommand<Vector>() {
		public Vector doIt(Connection con) throws SQLException {
		    return getBestSellers(con, subject);
		}
	    });
	return vector;
    }
    private static Vector getBestSellers(Connection con, String subject) throws SQLException {
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
	return vec;	
    }

    public static void getRelated(final int i_id, final Vector i_id_vec, final Vector i_thumbnail_vec) {
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    getRelated(con, i_id, i_id_vec, i_thumbnail_vec);
		    return VOID;
		}
	    });
    }
    private static void getRelated(Connection con, int i_id, Vector i_id_vec, Vector i_thumbnail_vec) throws SQLException {
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
	    
	    Calendar calendar = Calendar.getInstance();
	    long pubdate =     calendar.getTimeInMillis();
	    try{
	    con.executeUpdate(DBTPCWAdminUpdate.createOperation(i_id, cost, image, thumbnail, pubdate));
	    }catch (IOException e) {
	    	System.err.println("adminUpdate - IO exception when creating the shadow operation");
			throw new SQLException("adminUpdate - IO exception when creating the shadow operation");
	    }
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
	    try{
	    con.executeUpdate(DBTPCWAdminUpdateRelated.createOperation(i_id, related_items[0], related_items[1], related_items[2], related_items[3], related_items[4]));
	    }catch(IOException e){
	    	System.err.println("adminUpdateRelated - IO exception when creating the shadow operation");
			throw new SQLException("adminUpdateRelated - IO exception when creating the shadow operation");
	    }
    }

    public static String GetUserName(final int C_ID){
	String string = withTransaction(new tx.TransactionalCommand<String>() {
		public String doIt(Connection con) throws SQLException {
		    return GetUserName(con, C_ID);
		}
	    });
	return string;
    }
    private static String GetUserName(Connection con, int C_ID)throws SQLException {
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
	return u_name;
    }

    public static String GetPassword(final String C_UNAME){
	String string = withTransaction(new tx.TransactionalCommand<String>() {
		public String doIt(Connection con) throws SQLException {
		    return GetPassword(con, C_UNAME);
		}
	    });
	return string;
    }
    private static String GetPassword(Connection con, String C_UNAME)throws SQLException {
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
	return passwd;
    }

    //This function gets the value of I_RELATED1 for the row of
    //the item table corresponding to I_ID
    private static int getRelated1(int I_ID, Connection con) throws SQLException {
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
		    return GetMostRecentOrder(con, c_uname, order_lines);
		}
	    });
	return order;
    }
    private static Order GetMostRecentOrder(Connection con, String c_uname, Vector order_lines)throws SQLException {
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
	int SHOPPING_ID;
	
		int coin = TPCW_Util.getRandom(2);
		if(coin == 2){
			SHOPPING_ID = ShoppingIDFactory.addAndGet(totalproxies);
			Calendar calendar = Calendar.getInstance();
			try{
			con.executeUpdate(DBTPCWCreateEmptyCart.createOperation(SHOPPING_ID,calendar.getTimeInMillis()));
			}catch (IOException e) {
				System.err.println("Create empty cart - IO exception when creating the shadow operation");
				e.printStackTrace();
				throw new SQLException("Create empty cart - IO exception when creating the shadow operation");
			}
		}
		else{ 
			PreparedStatement get_cart_sharedcart = con.prepareStatement(@sql.getSharedCart.txmud@);
			get_cart_sharedcart.setInt(1,TPCW_Database.globalProxyId);
			ResultSet rs = get_cart_sharedcart.executeQuery();
			if(rs.next())
				SHOPPING_ID = rs.getInt(1);
			else{
				System.err.println("There's no shared shopping cart available, creating a new one");
				SHOPPING_ID = ShoppingIDFactory.addAndGet(totalproxies);
			}
		
		}
	    
	return SHOPPING_ID;
    }
    
    public static Cart doCart(final int SHOPPING_ID, final Integer I_ID, final Vector ids, final Vector quantities) {	
	Cart cart = withTransaction(new tx.TransactionalCommand<Cart>() {
		public Cart doIt(Connection con) throws SQLException {
		    return doCart(con, SHOPPING_ID, I_ID, ids, quantities);
		}
	    });
	return cart;
    }
    private static Cart doCart(Connection con, int SHOPPING_ID, Integer I_ID, Vector ids, Vector quantities) throws SQLException {
	    Cart cart = null;

	    if (I_ID != null) {
		addItem((TxMudConnection)con, SHOPPING_ID, I_ID.intValue()); 
	    }
	    refreshCart((TxMudConnection)con, SHOPPING_ID, ids, quantities);
	    addRandomItemToCartIfNecessary(con, SHOPPING_ID);
	    resetCartTime((TxMudConnection)con, SHOPPING_ID);
	    cart = TPCW_Database.getCart(con, SHOPPING_ID, 0.0);
	    
	return cart;
    }

    //This function finds the shopping cart item associated with SHOPPING_ID
    //and I_ID. If the item does not already exist, we create one with QTY=1,
    //otherwise we increment the quantity.

    private static void addItem(TxMudConnection con, int SHOPPING_ID, int I_ID) throws SQLException {
	    PreparedStatement find_entry = con.prepareStatement
		(@sql.addItem@);
	    
	    // Set parameter
	    find_entry.setInt(1, SHOPPING_ID);
	    find_entry.setInt(2, I_ID);
	    ResultSet rs = find_entry.executeQuery();
	    try{
		    // Results
		    if(rs.next()) {
		    	int currqty = rs.getInt("scl_qty");
		    	//we should add the delta to current local state
		    	//the reason we're passing the current quantity is to avoid issue the select query again 
		    	//for the local datacenter, and we dont want to ship too much of the application logic
		    	//to storageshim to avoid overloading
			    
			    	con.executeUpdate(DBTPCWAddItem.createOperation(SHOPPING_ID,I_ID,currqty,1));
			    
		    } else {//We need to add a new row to the table.
		    	con.executeUpdate(DBTPCWAddItem.createOperation(SHOPPING_ID,I_ID,0,1));
		    }
		    rs.close();
		    find_entry.close();
	    }catch (IOException e) {
			System.err.println("addItem - IO exception when creating the shadow operation");
			throw new SQLException("addItem - IO exception when creating the shadow operation");
		}
    }

    private static void refreshCart(TxMudConnection con, int SHOPPING_ID, Vector ids, 
				    Vector quantities) throws SQLException {
	int i;
	HashMap<Integer, Integer> current_quantity = new HashMap<Integer, Integer>();
	Vector<Integer> deltas = new Vector<Integer>();
	
	String str="SELECT i_id, i_qty FROM item ";
	StringBuffer addWhere= new StringBuffer();
	addWhere.append(" WHERE i_id in (");
	for(i = 0; i < ids.size(); i++){
		addWhere.append(ids.elementAt(i)+",");
	}
	str+=addWhere.substring(0, addWhere.length()-2)+")";
	
	Statement st = con.createStatement();
	ResultSet rs0 = st.executeQuery(str);
	while(rs0.next()){
		current_quantity.put(rs0.getInt(1), rs0.getInt(2));
	}
	 	
    try{
		for(i = 0; i < ids.size(); i++){
			String I_IDstr = (String) ids.elementAt(i);
			String QTYstr = (String) quantities.elementAt(i); // new quantities
			int I_ID = Integer.parseInt(I_IDstr);
			int QTY = Integer.parseInt(QTYstr);
			
			con.executeUpdate(DBTPCWRefreshCart.createOperation(SHOPPING_ID,I_ID,current_quantity.get(I_ID),QTY-current_quantity.get(I_ID)));
		 }
    }catch (IOException e) {
		System.err.println("refreshCart - IO exception when creating the shadow operation");
		throw new SQLException("refreshCart - IO exception when creating the shadow operation");
	}
    
    }

    private static void addRandomItemToCartIfNecessary(Connection con, int SHOPPING_ID) throws SQLException {
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
			addItem((TxMudConnection)con, SHOPPING_ID, related_item);
	    }
	    
	    rs.close();
	    get_cart.close();
    }


    // Only called from this class 
    private static void resetCartTime(TxMudConnection con, int SHOPPING_ID) throws SQLException {
	    Calendar calendar = Calendar.getInstance();
	    try{   
	    	con.executeUpdate(DBTPCWResetCartTime.createOperation(SHOPPING_ID,calendar.getTimeInMillis()));
	    }catch (IOException e) {
	  		System.err.println("resetCartTime - IO exception when creating the shadow operation");
	  		throw new SQLException("resetCartTime - IO exception when creating the shadow operation");
	  	}
    }

    public static Cart getCart(final int SHOPPING_ID, final double c_discount) {
	Cart cart = withTransaction(new tx.TransactionalCommand<Cart>() {
		public Cart doIt(Connection con) throws SQLException {
		    return getCart(con, SHOPPING_ID, c_discount);
		}
	    });
	return cart;
    }
    //time .05s
    private static Cart getCart(Connection con, int SHOPPING_ID, double c_discount) throws SQLException {
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
		    refreshSession(con, C_ID);
		    return VOID;
		}
	    });
    }
    private static void refreshSession(Connection con, int C_ID) throws SQLException {
	    PreparedStatement updateLogin = con.prepareStatement
		(@sql.refreshSession.txmud@);
	    
	    // Set parameter
	    Calendar calendar = Calendar.getInstance();
	    updateLogin.setDate(1, new java.sql.Date(calendar.getTimeInMillis()));
	    calendar.add(Calendar.HOUR,2);
	    updateLogin.setDate(2, new java.sql.Date(calendar.getTimeInMillis()));
	    updateLogin.setInt(3, C_ID);
	    updateLogin.executeUpdate();
	    
	    updateLogin.close();
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
	    insert_customer_row.setDate(9, new 
					java.sql.Date(cust.c_since.getTime()));
	    insert_customer_row.setDate(10, new java.sql.Date(cust.c_last_visit.getTime()));
	    insert_customer_row.setDate(11, new java.sql.Date(cust.c_login.getTime()));
	    insert_customer_row.setDate(12, new java.sql.Date(cust.c_expiration.getTime()));
	    insert_customer_row.setDouble(13, cust.c_discount);
	    insert_customer_row.setDouble(14, cust.c_balance);
	    insert_customer_row.setDouble(15, cust.c_ytd_pmt);
	    insert_customer_row.setDate(16, new java.sql.Date(cust.c_birthdate.getTime()));
	    insert_customer_row.setString(17, cust.c_data);
	
	    cust.addr_id = enterAddress(con, 
					cust.addr_street1, 
					cust.addr_street2,
					cust.addr_city,
					cust.addr_state,
					cust.addr_zip,
					cust.co_name);
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
		    return doBuyConfirm((Connection)con, shopping_id, customer_id, cc_type, cc_number, cc_name, cc_expiry, shipping);
		}
	    });
	return buyConfirmResult;
    }
    private static BuyConfirmResult doBuyConfirm(Connection con, int shopping_id, int customer_id, String cc_type, long cc_number,
						 String cc_name, Date cc_expiry, String shipping) throws SQLException {
	
	BuyConfirmResult result = new BuyConfirmResult();
	    double c_discount = getCDiscount(con, customer_id);
	    result.cart = getCart(con, shopping_id, c_discount);
	    if(result.cart.lines.size()==0){//cart was cleaned when the remote party purchased the items
	    	result.cart = getClosedCart(con,shopping_id);
	    	result.order_id=result.cart.SC_ORDER_ID;
	    	return result;    	
	    }
	    int ship_addr_id = getCAddr(con, customer_id);
	    result.order_id = enterOrder((TxMudConnection)con, customer_id, result.cart, ship_addr_id, shipping, c_discount);
	    enterCCXact((TxMudConnection)con, result.order_id, cc_type, cc_number, cc_name, cc_expiry, result.cart.SC_TOTAL, ship_addr_id);
	    clearCart((TxMudConnection)con, shopping_id,result.order_id);
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
    private static BuyConfirmResult doBuyConfirm(Connection con, int shopping_id, int customer_id, String cc_type, long cc_number,
						 String cc_name, Date cc_expiry, String shipping, String street_1, String street_2,
						 String city, String state, String zip, String country) throws SQLException {
	BuyConfirmResult result = new BuyConfirmResult();
	    double c_discount = getCDiscount(con, customer_id);
	    result.cart = getCart(con, shopping_id, c_discount);
	    if(result.cart.lines.size()==0){//cart was cleaned when the remote party purchased the items
	    	result.cart = getClosedCart(con,shopping_id);
	    	result.order_id=result.cart.SC_ORDER_ID;
	    	return result;    	
	    }
	    int ship_addr_id = enterAddress((TxMudConnection)con, street_1, street_2, city, state, zip, country);
	    result.order_id = enterOrder((TxMudConnection)con, customer_id, result.cart, ship_addr_id, shipping, c_discount);
	    enterCCXact((TxMudConnection)con, result.order_id, cc_type, cc_number, cc_name, cc_expiry, result.cart.SC_TOTAL, ship_addr_id);
	    clearCart((TxMudConnection)con, shopping_id,result.order_id);
	return result;
    }


    //DB query time: .05s
    public static double getCDiscount(Connection con, int c_id) throws SQLException {
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
    public static int getCAddrID(Connection con, int c_id) throws SQLException {
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

    public static int getCAddr(Connection con, int c_id) throws SQLException {
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
				   int ship_addr_id) throws SQLException {

	// Updates the CC_XACTS table
	if(cc_type.length() > 10)
	    cc_type = cc_type.substring(0,10);
	if(cc_name.length() > 30)
	    cc_name = cc_name.substring(0,30);
	
	long cc_expiry1 = cc_expiry.getTime();
	long cc_xact_date = System.currentTimeMillis();
	try{
	con.executeUpdate(DBTPCWEnterCCXact.createOperation(o_id, cc_type, cc_number, cc_name, cc_expiry1, total, cc_xact_date, ship_addr_id));
	}catch(IOException e){
		System.err.println("EnterCCXact - IO exception when creating the shadow operation");
  		throw new SQLException("EnterCCXact - IO exception when creating the shadow operation");
	}
	}
    
    public static void clearCart(TxMudConnection con, int shopping_id, int order_id) throws SQLException {
	// Empties all the lines from the shopping_cart_line for the
	// shopping id.  Does not remove the actually shopping cart
	    try{
	    con.executeUpdate(DBTPCWClearCart.createOperation(shopping_id, order_id));
	    }catch(IOException e){
	    	System.err.println("clearCart - IO exception when creating the shadow operation");
	  		throw new SQLException("clearCart - IO exception when creating the shadow operation");
	    }
	    System.err.println("Shopping cart is closed!");
    }

    public static int enterAddress(TxMudConnection con,  // Do we need to do this as part of a transaction?
				   String street1, String street2,
				   String city, String state,
				   String zip, String country) throws SQLException {
	// returns the address id of the specified address.  Adds a
	// new address to the table if needed
	int addr_id = 0;

        // Get the country ID from the country table matching this address.

        // Is it safe to assume that the country that we are looking
        // for will be there
	    PreparedStatement get_co_id = con.prepareStatement
		(@sql.enterAddress.id@);
	    get_co_id.setString(1, country);
	    ResultSet rs = get_co_id.executeQuery();
	    rs.next();
	    int addr_co_id = rs.getInt("co_id");
	    rs.close();
	    get_co_id.close();
	    
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

	    addr_id = AddrIDFactory.addAndGet(totalproxies);
	    //Need to insert a new row in the address table
	    try{
		con.executeUpdate(DBTPCWEnterAddress.createOperation(addr_id, street1, street2, city, state, zip, addr_co_id));
	    }catch(IOException e){
	    	System.err.println("EnterAddress - IO exception when creating the shadow operation");
	  		throw new SQLException("EnterAddress - IO exception when creating the shadow operation");
	    }
	    } else { //We actually matched
		addr_id = rs.getInt("addr_id");
	    }
	    match_address.close();
	    rs.close();
	return addr_id;
    }

 
    public static int enterOrder(TxMudConnection con, int customer_id, Cart cart, int ship_addr_id, String shipping, double c_discount) throws SQLException {
	// returns the new order_id
    	int o_id = 0;
		Calendar calendar = Calendar.getInstance();
		// - Creates an entry in the 'orders' table 
		long o_date = calendar.getTimeInMillis();
		calendar.add(Calendar.DAY_OF_YEAR, TPCW_Util.getRandom(7));
		long o_ship_date = calendar.getTimeInMillis();
	    
		int o_bill_addr_id =getCAddrID((Connection)con, customer_id);
		double o_sub_total= cart.SC_SUB_TOTAL;
	    double o_total = cart.SC_TOTAL;
		o_id = OrderLineIDFactory.addAndGet(totalproxies);
	    try{
	    con.executeUpdate(DBTPCWEnterOrder.createOperation(o_id, customer_id, o_date, o_sub_total, o_total, shipping, o_ship_date, o_bill_addr_id, ship_addr_id));
	    }catch(IOException e){
	    	System.err.println("EnterOrder - IO exception when creating the shadow operation");
	  		throw new SQLException("EnterOrder - IO exception when creating the shadow operation");
	    }
	Enumeration e = cart.lines.elements();
	int counter = 0;
	while(e.hasMoreElements()) {
	    // - Creates one or more 'order_line' rows.
	    CartLine cart_line = (CartLine) e.nextElement();
	    addOrderLine(con, counter, o_id, cart_line.scl_i_id, 
			 cart_line.scl_qty, c_discount, 
			 TPCW_Util.getRandomString(20, 100));
	    counter++;

	    // - Adjusts the stock for each item ordered
	    int stock = getStock(con, cart_line.scl_i_id);
	    if ((stock - cart_line.scl_qty) < 10) {
		setStock(con, cart_line.scl_i_id, 
			 stock - cart_line.scl_qty + 21);
	    } else {
		setStock(con, cart_line.scl_i_id, stock - cart_line.scl_qty);
	    }
	}
	return o_id;
    }
    
    public static void addOrderLine(TxMudConnection con, 
				    int ol_id, int ol_o_id, int ol_i_id, 
				    int ol_qty, double ol_discount, String ol_comment) throws SQLException {
	int success = 0;
	try{
	    con.executeUpdate(DBTPCWAddOrderLine.createOperation(ol_id, ol_o_id, ol_i_id, ol_qty, ol_discount, ol_comment));
	}catch(IOException e){
		System.err.println("AddOrderLine - IO exception when creating the shadow operation");
  		throw new SQLException("AddOrderLine - IO exception when creating the shadow operation");
	}
    }

    public static int getStock(Connection con, int i_id) throws SQLException {
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
	    try{
    	con.executeUpdate(DBTPCWSetStock.createOperation(i_id, new_stock));
	    }catch(IOException e){
	    	System.err.println("SetStock - IO exception when creating the shadow operation");
	  		throw new SQLException("SetStock - IO exception when creating the shadow operation");
	    }
    }

    public static void verifyDBConsistency(){
	withTransaction(new tx.TransactionalCommand<Void>() {
		public Void doIt(Connection con) throws SQLException {
		    verifyDBConsistency(con);
		    return VOID;
		}
	    });
    }
    private static void verifyDBConsistency(Connection con) throws SQLException {
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
    
    private static Cart getClosedCart(Connection con,int shopping_id) throws SQLException{
    	Cart mycart = new Cart();
    	PreparedStatement get_closed_cart_order = con.prepareStatement(@sql.getClosedCart.order@);
    	get_closed_cart_order.setInt(1, shopping_id);
	    ResultSet rs = get_closed_cart_order.executeQuery();
	    int order_id=-1;
	    if(rs.next())
	    	mycart.SC_ORDER_ID=rs.getInt(1);
	    else
	    	System.err.println("Error, cart is not closed therefore has no order yet!!!");
	    rs.close();
	    get_closed_cart_order.close();
	    
	    
	    PreparedStatement get_closed_cart_details = con.prepareStatement(@sql.getClosedCart.details@);
    	get_closed_cart_details.setInt(1, order_id);
	    ResultSet rs_details = get_closed_cart_details.executeQuery();
	    
	    if(rs_details.next()){
	    	mycart.SC_SUB_TOTAL=rs_details.getDouble("o_sub_total");
		    mycart.SC_TAX=rs_details.getDouble("o_tax");
		    mycart.SC_TOTAL=rs_details.getDouble("o_total");
		    mycart.SC_SHIP_COST=mycart.SC_TOTAL-mycart.SC_SUB_TOTAL-mycart.SC_TAX;
	    }
	    else
	    	System.err.println("Error, cart is not closed therefore has no order yet!!!");
	    rs_details.close();
	    get_closed_cart_details.close();
	    
	    //////////////
	    PreparedStatement get_closed_cart_items = con.prepareStatement(@sql.getClosedCart.items@);
	    get_closed_cart_items.setInt(1, order_id);
	    ResultSet rs_items = get_closed_cart_items.executeQuery();
	    
	    while(rs_items.next()){
	    	CartLine line = new CartLine(
	    			rs_items.getString("i_title"),
		 		 	rs_items.getDouble("i_cost"),
					rs_items.getDouble("i_srp"),
					rs_items.getString("i_backing"),
	    			rs_items.getInt("ol_qty"),
					rs_items.getInt("ol_i_id"));
			mycart.lines.addElement(line);
	    }
	    rs_items.close();
	    get_closed_cart_items.close();
    
	    return mycart;
    }
        
    public synchronized static void initDatabasePool(){
    	if(use_connection_pool==false || pool_initialized==true)
    		return;

    	Vector<Connection> connections = new Vector<Connection>();
    	TxMudDriver.proxy = proxy.imp;
    	int i=0;
    	try{
	    	for(i=0;i<maxConn-availConn.size();i++){
	    		System.err.println("Initializing database pool. connection:"+(i+1));
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
    	Connection con = getConnection();
    	int n;
    	TPCW_Database.globalProxyId=globalProxyId;
    	try{
    	PreparedStatement get_next_id = con.prepareStatement(@sql.createEmptyCart@);
    	ResultSet rs = get_next_id.executeQuery();
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
		n= n - (n % totalproxies) + totalproxies + globalProxyId;
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
		n= n - (n % totalproxies) + totalproxies + globalProxyId;
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
    	n= n - (n % totalproxies) + totalproxies + globalProxyId;
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
		n= n - (n % totalproxies) + totalproxies + globalProxyId;
    	System.err.println("Proxy["+proxyId+"] set initial Customer ID:"+n);
		CustomerIDFactory.set(n);
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
	boolean tcpnodelay=true ;
	ClosedLoopProxy imp; 
	NettyTCPSender sendNet;
	ParallelPassThroughNetworkQueue ptnq;
	NettyTCPReceiver rcv;

	public TPCW_TxMud_Proxy(int dcid, int proxyid, int threads, String file){
		this.proxy_cnf=file; 
		this.dcId=dcid;
		this.proxyId=proxyid;
		this.proxyThreads=threads;
		this.imp = new ClosedLoopProxy(proxy_cnf, dcId, proxyId, this);
		// set up the networking channels
		//sender
		sendNet = new NettyTCPSender();
		imp.setSender(sendNet);
		sendNet.setTCPNoDelay(tcpnodelay);
		//receiver
		ptnq = new ParallelPassThroughNetworkQueue(imp, proxyThreads);
		rcv = new NettyTCPReceiver(imp.getMembership().getMe().getInetSocketAddress(), ptnq, 2);
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

