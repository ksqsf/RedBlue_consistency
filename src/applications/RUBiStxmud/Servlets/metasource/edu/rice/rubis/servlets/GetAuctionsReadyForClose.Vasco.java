package edu.rice.rubis.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mpi.vasco.txstore.scratchpad.rdbms.jdbc.TxMudConnection;

/** Builds the html page with the list of a set of items
 * that are ready for closing, each item must have bids, and its
 * end-date is not reaching yet. */
public class GetAuctionsReadyForClose extends HttpServlet
{

  /** Build the html page for the response */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    ServletPrinter sp = null;
    PreparedStatement stmt = null;
    TxMudConnection conn = null;
    Integer maxIdOfOpenItems;

    sp = new ServletPrinter(response, "GetAuctionsReadyForClose");
    sp.printHTMLheader("RUBiS available auctions for closing");


    
    String strOfMaxIdOfOpenItems = request.getParameter("maxOpenItemId");
    if ((strOfMaxIdOfOpenItems == null) || (strOfMaxIdOfOpenItems.equals("")))
    {
      sp.printHTML("<h3>You must provide the max id of open items !<br></h3>");
      sp.printHTMLfooter();
      return;
    }
    
    maxIdOfOpenItems = new Integer(strOfMaxIdOfOpenItems);
    
    conn = Database.getConnection();

    ResultSet rs = null;
    
    // get the region ID
    try
    {
    	//add randomness here
    	String now = TimeManagement.currentDateToString();
        stmt =
            conn.prepareStatement(
              "SELECT items.name, items.id, items.end_date, items.max_bid, items.nb_of_bids FROM items WHERE id < ? AND end_date>= '"+now+"' AND items.nb_of_bids > 0 AND RAND()<=0.09 LIMIT ?");
        stmt.setInt(1, maxIdOfOpenItems.intValue());
        stmt.setInt(2, 50);
        rs = stmt.executeQuery();
        if (!rs.first())
        {
         sp.printHTML(
           " No active auctions in the database!<br>");
         Database.commit(conn);
         Database.closeConnection(stmt, conn);
         return;
       }
        sp.printItemHeaderForClose();
        do
        {
          String itemName = rs.getString("name");
          int itemId = rs.getInt("id");
          String endDate = rs.getString("end_date");
          float maxBid = rs.getFloat("max_bid");
          int nbOfBids = rs.getInt("nb_of_bids");
          sp.printItemForClose(itemName, itemId, maxBid, nbOfBids, endDate);
        }
        while (rs.next());
      stmt.close();
    }
    catch (SQLException e)
    {
      sp.printHTML("Failed to execute Query for getting auctions for close " + e);
      Database.rollback(conn);
      Database.closeConnection(stmt, conn);
      return;
    }
    
    Database.commit(conn);
    Database.closeConnection(stmt, conn);
    
    sp.printItemFooter();
    sp.printHTMLfooter();

  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    doGet(request, response);
  }

  /**
   * Clean up the connection pool.
   */
  public void destroy()
  {
    super.destroy();
  }

}
