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

import txstore.scratchpad.rdbms.jdbc.TxMudConnection;
import txstore.scratchpad.rdbms.util.rubis.DBRUBISShdEmpty;

/** Builds the html page with the list of all categories and provides links to browse all
    items in a category or items in a category for a given region */
public class BrowseCategories extends HttpServlet
{
  


//  public int getPoolSize()
//  {
//    return Config.BrowseCategoriesPoolSize;
//  }
//  

  /** List all the categories in the database */
  private boolean categoryList(int regionId, int userId, PreparedStatement stmt, TxMudConnection conn, ServletPrinter sp)
  {
    String categoryName;
    int categoryId;
    ResultSet rs = null;

    // get the list of categories
    try
    {
      stmt = conn.prepareStatement("SELECT name, id FROM categories");
      rs = stmt.executeQuery();
    }
    catch (Exception e)
    {
      sp.printHTML("Failed to execute Query for categories list: " + e);
      Database.rollback(conn);
      Database.closeConnection(stmt, conn);
      return false;
    }
    try
    {
      if (!rs.first())
      {
        sp.printHTML(
          "<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>");
        //set shadow operation
        try{
    	    DBRUBISShdEmpty dEm = DBRUBISShdEmpty.createOperation();
    	    conn.setShadowOperation(dEm, 0);
        }catch (IOException e){
    		// TODO Auto-generated catch block
    		e.printStackTrace();
        }
        Database.commit(conn);
        Database.closeConnection(stmt, conn);
        return false;
      }
      else
        sp.printHTML("<h2>Currently available categories</h2><br>");

      do
      {
        categoryName = rs.getString("name");
        categoryId = rs.getInt("id");
        if (regionId != -1)
        {
          sp.printCategoryByRegion(categoryName, categoryId, regionId);
        }
        else
        {
          if (userId != -1)
            sp.printCategoryToSellItem(categoryName, categoryId, userId);
          else
            sp.printCategory(categoryName, categoryId);
        }
      }
      while (rs.next());
    }
    catch (Exception e)
    {
      sp.printHTML("Exception getting categories list: " + e + "<br>");
      Database.rollback(conn);
      Database.closeConnection(stmt, conn);
      return false;
    }
    return true;
  }

  /** Build the html page for the response */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
  {
    ServletPrinter sp = null;
    PreparedStatement stmt = null;
    TxMudConnection conn = null;
    int regionId = -1, userId = -1;
    String username = null, password = null;

    sp = new ServletPrinter(response, "BrowseCategories");
    sp.printHTMLheader("RUBiS available categories");

    username = request.getParameter("nickname");
    password = request.getParameter("password");

    conn = Database.getConnection();

    // Authenticate the user who want to sell items
    if ((username != null && username != "")
      || (password != null && password != ""))
    {
      Auth auth = new Auth(conn, sp);
      userId = auth.authenticate(username, password);
      if (userId == -1)
      {
        sp.printHTML(
          " You don't have an account on RUBiS!<br>You have to register first.<br>");
        sp.printHTMLfooter();
        //set shadow operation
        try{
    	    DBRUBISShdEmpty dEm = DBRUBISShdEmpty.createOperation();
    	    conn.setShadowOperation(dEm, 0);
        }catch (IOException e){
    		// TODO Auto-generated catch block
    		e.printStackTrace();
        }
        Database.commit(conn);
        Database.closeConnection(stmt, conn);
        return;
      }
    }

    String value = request.getParameter("region");
    if ((value != null) && (!value.equals("")))
    {
      // get the region ID
      try
      {
        stmt = conn.prepareStatement("SELECT id FROM regions WHERE name=?");
        stmt.setString(1, value);
        ResultSet rs = stmt.executeQuery();
        if (!rs.first())
        {
          sp.printHTML(
            " Region " + value + " does not exist in the database!<br>");
          //set shadow operation
          try{
      	    DBRUBISShdEmpty dEm = DBRUBISShdEmpty.createOperation();
      	    conn.setShadowOperation(dEm, 0);
          }catch (IOException e){
      		// TODO Auto-generated catch block
      		e.printStackTrace();
          }
          Database.commit(conn);
          Database.closeConnection(stmt, conn);
          return;
        }
        regionId = rs.getInt("id");
        stmt.close();
      }
      catch (SQLException e)
      {
        sp.printHTML("Failed to execute Query for region: " + e);
        Database.rollback(conn);
        Database.closeConnection(stmt, conn);
        return;
      }
    }

    boolean connAlive = categoryList(regionId, userId, stmt, conn, sp);
    if (connAlive) {
        //set shadow operation
        try{
    	    DBRUBISShdEmpty dEm = DBRUBISShdEmpty.createOperation();
    	    conn.setShadowOperation(dEm, 0);
        }catch (IOException e){
    		// TODO Auto-generated catch block
    		e.printStackTrace();
        }
    	Database.commit(conn);
        Database.closeConnection(stmt, conn);
    }
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
