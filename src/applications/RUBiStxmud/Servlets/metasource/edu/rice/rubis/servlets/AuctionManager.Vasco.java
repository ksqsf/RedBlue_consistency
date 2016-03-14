package edu.rice.rubis.servlets;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mpi.vasco.network.ParallelPassThroughNetworkQueue;
import org.mpi.vasco.network.netty.NettyTCPReceiver;
import org.mpi.vasco.network.netty.NettyTCPSender;
import org.mpi.vasco.txstore.appextend.ExecuteScratchpadFactory;

import org.mpi.vasco.txstore.scratchpad.rdbms.jdbc.TxMudConnection;
import org.mpi.vasco.txstore.proxy.ApplicationInterface;
import org.mpi.vasco.txstore.proxy.ClosedLoopProxy;
import org.mpi.vasco.txstore.util.Operation;
import org.mpi.vasco.util.debug.Debug;

public class AuctionManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	//private static ScheduledThreadPoolExecutor closeAuctionPool = new ScheduledThreadPoolExecutor(100);
	//static Hashtable<Integer,CloseAuctionOperation> ActiveItems = new Hashtable<Integer,CloseAuctionOperation>(10000); 
	//static AtomicInteger closedAuctions = new AtomicInteger(0);

	// initialize the database pool upon a loads up
	public void init() throws ServletException{
		// reset values after reading from topology file
		Config.TotalProxies = 1;
		Config.MaxProxyPerDatacenter = 1;
		Config.TotalDatacenters = 1;
		Config.DatacenterID = 0;
		Config.ProxyID = 0; // compute non-overlap proxy id
		ServletContext context = getServletContext();
		String HTMLFilesPath = "";
		String DatabaseProperties = "WEB-INF/classes/mysql.properties";
		Config.HTMLFilesPath = context.getRealPath("") + "/" + HTMLFilesPath;
		Config.DatabaseProperties = context.getRealPath("") + "/"+ DatabaseProperties;
		
		//reading parameters
		Config.TotalProxies = Integer.parseInt(context.getInitParameter("totalproxy"));
		System.out.println("total proxy "+ Config.TotalProxies);
		Config.TotalDatacenters = Integer.parseInt(context.getInitParameter("dcCount"));
		System.out.println("total dc "+ Config.TotalDatacenters);
		Config.DatacenterID = Integer.parseInt(context.getInitParameter("dcId"));
		System.out.println("dcId "+ Config.DatacenterID);
		Config.ProxyID = Integer.parseInt(context.getInitParameter("proxyId"));
		System.out.println("proxyId "+ Config.ProxyID);
		Config.DatabasePool = Integer.parseInt(context.getInitParameter("dbpool"));
		System.out.println("db pool "+ Config.DatabasePool);
		@initializeBackend@

	}

	public void destroy() {
		//closeAuctionPool.shutdown();
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
   
	    int i;
	    PrintWriter out = res.getWriter();
	    long now=0;
	    res.setContentType("text/plain");
	    HttpSession session = req.getSession(false);
	    String command  = req.getParameter("command");
	    if(command.equals("getAborts")){
	    	int aborts= Database.aborts.get();
			out.println("aborts "+aborts);
	    }
	    if(command.equals("setProxyId")){
	    	String proxyid = req.getParameter("proxyid");
	    	Config.ProxyID=Integer.parseInt(proxyid);
	    	String totalproxies= req.getParameter("totalproxies");
	    	Config.TotalProxies=Integer.parseInt(totalproxies);
	    	out.println("proxy_number "+Config.ProxyID);
	    	
	    }
	    if(command.equals("getTransactions")){
	    	int transactions= Database.transactions;
			out.println("transactions "+transactions);
	    }
	    if(command.equals("getCommitedTransactions")){
	    	int transactions= Database.commitedtxn.get();
			out.println("transactions "+transactions);
	    }
	    if(command.equals("configure")){//restart an experiment
	    	Database.transactions=0;
	    	Database.aborts.set(0);
	    	Database.commitedtxn.set(0);
	    	
	    	now = System.currentTimeMillis();
	    	//String totalproxies= req.getParameter("totalproxies");
	    	//TPCW_Database.totalproxies=Integer.parseInt(totalproxies);
	    	String startmi= req.getParameter("startmi");
	    	Database.startmi=now+Long.parseLong(startmi);
	    	String duration= req.getParameter("duration");
	    	Database.endmi=Database.startmi+Long.parseLong(duration);
	    	
//	    	String globalProxyId = req.getParameter("proxyid");
//	    	System.err.println("Proxy global ID"+globalProxyId);
//	    	TPCW_Database.initID(Integer.parseInt(globalProxyId));
	    	
	    	@txmud.setmeasurementinterval@
	    	
	    	out.println("ok");
	    	out.flush();
	    	System.err.println("Proxy ["+Config.ProxyID+"] configured at "+new Date(now) +
	        		", it will start at "+new Date(Database.startmi)+" and stop at "+new Date(Database.endmi));	
	    }
	    if(command.equals("getTxMudAborts")){
	    	out.println("aborts "+@txmud.getaborts@);
	    }if(command.equals("getTxMudRedTransactions")){
	    	out.println("red "+@txmud.getredtxn@);
	    }if(command.equals("getTxMudBlueTransactions")){
	    	out.println("blue "+@txmud.getbluetxn@);
		
	    }
		
	}// do get

//	public static void scheduleCloseAuction(GregorianCalendar whenclose, int auctionID){
//		System.out.println("total active threads "+closeAuctionPool.getActiveCount());
//		long delay = (whenclose.getTimeInMillis() - System.currentTimeMillis()) * 1000;
//		if(delay > 0){	//ignore database population
//			CloseAuctionOperation operation = new CloseAuctionOperation(auctionID);
//			closeAuctionPool.schedule(operation, delay, TimeUnit.SECONDS);
//			ActiveItems.put(auctionID, operation);
//		}
//	}
//	public static void cancelScheduledCloseAuction(int auctionID){
//		closeAuctionPool.remove(ActiveItems.remove(auctionID));
//	}
	
	private synchronized void initializeVascoBackend() throws ServletException{
		//get context variables here
		System.out.println("initialize Vasco Proxy");
		Database.proxy= new RUBIS_TxMud_Proxy(Config.DatacenterID,Config.ProxyID,10,"rubis_txmud.xml", Config.TotalDatacenters, 0, "rubis_txmud_db.xml", 20);
		Database.init(); //initialize the connection pool
	}

//	/**
//	 * Close both statement and connection.
//	 */
//	protected void closeAuction(int auctionId) {
//		try {
//			Connection conn = Database.getConnection();
//			conn.setTransactionIsolation(conn.TRANSACTION_SERIALIZABLE);
//			String now = TimeManagement.currentDateToString();
//			
//			PreparedStatement stmt =
//		          conn.prepareStatement(
//		            "UPDATE items SET end_date=? WHERE id=?");
//		        stmt.setString(1, now);
//		        stmt.setInt(2, auctionId);
//		        stmt.executeUpdate();
//		        stmt.close();
//		    
//		        
//			conn.commit();
//			conn.setTransactionIsolation(conn.TRANSACTION_REPEATABLE_READ);
//			Database.releaseConnection(conn);
//
//		} catch (Exception e) {
//			System.err.println("exception found during close auction " + e.toString());
//			e.printStackTrace();
//		}
//	}
	
	
//	/**
//	 * Close both statement and connection.
//	 */
//	protected void selectWinner(int auctionId) {
//		try {
//			Connection conn = Database.getConnection();
//			String now = TimeManagement.currentDateToString();
//			
//			PreparedStatement stmt =
//		          conn.prepareStatement(
//		            "SELECT bids.id, bids.qty, bids.bid, bids.max_bid, items.reserve_price, items.quantity from bids, items WHERE bids.item_id = items.id AND items.id=? ORDER BY bids.max_id DESC");
//		        stmt.setInt(1, auctionId);
//		    ResultSet rs = stmt.executeQuery();
//		    int quantity = 0;
//		    float reserve_price;
//		    int count = 0;
//		    PreparedStatement storebidstmt = null;
//		    while(rs.next()){
//		    	if(count == 0){
//		    		quantity = rs.getInt("quantity");
//		    		reserve_price = rs.getFloat("reserve_price");
//		    	}
//		    	if(quantity == 0){
//		    		System.out.println("auction " + auctionId + " assigned up");
//		    	}
//		    	int qty = rs.getInt("qty");
//		    	float price = rs.getFloat("bid");
//		    	int bidId = rs.getInt("id");
//		    	if(qty <= quantity){
//					storebidstmt =
//				          conn.prepareStatement(
//				            "INSERT INTO winners values (?,?,?)");
//					storebidstmt.setInt(1, bidId);
//					storebidstmt.setInt(2, qty);
//					storebidstmt.setFloat(3, price);
//					quantity = quantity - qty;
//					storebidstmt.executeUpdate();
//		    	}else{
//		    		storebidstmt =
//				          conn.prepareStatement(
//				            "INSERT INTO winners values (?,?,?)");
//					storebidstmt.setInt(1, bidId);
//					storebidstmt.setInt(2, quantity);
//					storebidstmt.setFloat(3, price);
//					quantity = 0;
//					storebidstmt.executeUpdate();
//		    	}
//		    }
//		        stmt.close();
//		        storebidstmt.close();
//		        rs.close();
//		        
//			Database.commit(conn);
//			//conn.setTransactionIsolation(conn.TRANSACTION_REPEATABLE_READ);
//			Database.releaseConnection(conn);
//
//		} catch (Exception e) {
//			System.err.println("exception found during close auction " + e.toString());
//			e.printStackTrace();
//		}
//	}
	
}
//class CloseAuctionOperation implements Runnable {
//	private int auctionID;
//	AuctionManager proxy; 
//	public CloseAuctionOperation(int id){
//		auctionID = id;
//	}
//	
//	@Override
//	public void run() {
//		//issue the sql statement to set the auction closer and find the winner
//		proxy.closeAuction(auctionID);
//		System.out.println("Finishing auction "+auctionID);
//		System.out.println("Total closed auctions so far"+AuctionManager.closedAuctions.incrementAndGet());
//		proxy.selectWinner(auctionID);
//	}
//	
//	
//}

class RUBIS_TxMud_Proxy implements ApplicationInterface {
	String proxy_cnf=""; 
	int dcId=0;
	int proxyId=0;
	int proxyThreads=10; 
	boolean tcpnodelay=true ;
	ClosedLoopProxy imp; 
	NettyTCPSender sendNet;
	ParallelPassThroughNetworkQueue ptnq;
	NettyTCPReceiver rcv;

	public RUBIS_TxMud_Proxy(int dcid, int proxyid, int threads, String file, int c, int ssId, String dbXmlFile, int s){
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
	
	public int getTotalProxyNum() {
		int num = 0;
		for(int i = 0 ; i < this.imp.getDatacenterCount(); i++){
			num += this.imp.getMembership().getProxyCount(i);
		}
		return num;
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
