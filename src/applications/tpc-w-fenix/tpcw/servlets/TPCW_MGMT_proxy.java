/* 
 * TPCW_say_hello.java - Utility function used by home interaction, 
 *                       creates a new session id for new users.
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
 ************************************************************************/

import java.io.*;
import java.util.Date;

import javax.servlet.*;
import javax.servlet.http.*;

public class TPCW_MGMT_proxy extends HttpServlet{
    
	//initialize the database pool upon a loads up
	public void init(ServletConfig config) throws ServletException{
		super.init(config);
		ServletContext context=getServletContext();
		String xmlfile2=context.getRealPath("")+"/"+context.getInitParameter("dcXMLConfigFile");
		int dcid= Integer.parseInt(context.getInitParameter("dcId"));
		int proxyid=Integer.parseInt(context.getInitParameter("proxyId"));
		int nthreads=Integer.parseInt(context.getInitParameter("nthreads"));
		TPCW_Database.proxyId=proxyid;
		int dbpool=Integer.parseInt(context.getInitParameter("dbpool"));
		TPCW_Database.maxConn=dbpool;
		System.err.println("dcCount " + context.getInitParameter("dcCount"));
		int dcCount = Integer.parseInt(context.getInitParameter("dcCount"));
		System.err.println("ssId " + context.getInitParameter("ssId"));
		int ssId = Integer.parseInt(context.getInitParameter("ssId"));
		System.err.println("dbFile " + context.getInitParameter("dbFile"));
		String dbFile = context.getRealPath("")+"/"+context.getInitParameter("dbFile");
		System.err.println("scratchpadNum " + context.getInitParameter("scratchpadNum"));
		int scratchpadNum = Integer.parseInt(context.getInitParameter("scratchpadNum"));
		
		System.err.println("TxMuD topology description: "+xmlfile2);
		System.err.println("Set up data center id: "+dcid);
		System.err.println("Set up proxy id: "+proxyid);
		System.err.println("Set up connection pool: "+TPCW_Database.maxConn);
		System.err.println("Set up dcCount: " + dcCount);
		System.err.println("Set up ssId: " + ssId);
		System.err.println("Set up dbFile: " + dbFile);
		System.err.println("Set up scratchpadNum: " + scratchpadNum);
		@txmud.initializeproxy@
		
		TPCW_Database.initDatabasePool();
		
		
		int totaldc = Integer.parseInt(context.getInitParameter("totaldc"));
		TPCW_Database.totalproxies=Integer.parseInt(context.getInitParameter("totalproxy"));
    	TPCW_Database.initID(proxyid+TPCW_Database.totalproxies*dcid);
		
	}
	public void destroy(){
		//TODO` TPCW_Database.destroyDatabasePool();
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
    	int aborts= TPCW_Database.aborts.get();
		out.println("aborts "+aborts);
    }
    if(command.equals("setProxyId")){
    	String proxyid = req.getParameter("proxyid");
    	TPCW_Database.proxyId=Integer.parseInt(proxyid);
    	String totalproxies= req.getParameter("totalproxies");
    	TPCW_Database.totalproxies=Integer.parseInt(totalproxies);
    	out.println("proxy_number "+TPCW_Database.proxyId);
    	
    }
    if(command.equals("getTransactions")){
    	int transactions= TPCW_Database.transactions;
		out.println("transactions "+transactions);
    }
    if(command.equals("getCommitedTransactions")){
    	int transactions= TPCW_Database.commitedtxn.get();
		out.println("transactions "+transactions);
    }
    if(command.equals("configure")){//restart an experiment
    	TPCW_Database.transactions=0;
    	TPCW_Database.aborts.set(0);
    	TPCW_Database.commitedtxn.set(0);
    	
    	now = System.currentTimeMillis();
    	//String totalproxies= req.getParameter("totalproxies");
    	//TPCW_Database.totalproxies=Integer.parseInt(totalproxies);
    	String startmi= req.getParameter("startmi");
    	TPCW_Database.startmi=now+Long.parseLong(startmi);
    	String duration= req.getParameter("duration");
    	TPCW_Database.endmi=TPCW_Database.startmi+Long.parseLong(duration);
    	
//    	String globalProxyId = req.getParameter("proxyid");
//    	System.err.println("Proxy global ID"+globalProxyId);
//    	TPCW_Database.initID(Integer.parseInt(globalProxyId));
    	
    	@txmud.setmeasurementinterval@
    	
    	out.println("ok");
    	out.flush();
    	System.err.println("Proxy ["+TPCW_Database.proxyId+"] configured at "+new Date(now) +
        		", it will start at "+new Date(TPCW_Database.startmi)+" and stop at "+new Date(TPCW_Database.endmi));	
    }
    if(command.equals("getTxMudAborts")){
    	out.println("aborts "+@txmud.getaborts@);
    }if(command.equals("getTxMudRedTransactions")){
    	out.println("red "+@txmud.getredtxn@);
    }if(command.equals("getTxMudBlueTransactions")){
    	out.println("blue "+@txmud.getbluetxn@);
    }
    
    }//do get

}


