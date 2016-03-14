package applications.microbenchmark.TxMudTest;
import txstore.scratchpad.rdbms.DBScratchpad;
import util.Debug;

import java.sql.*;

public class TestMimerSql
{

	public static void main( String[] args) {
		try {
		      Class.forName("com.mimer.jdbc.Driver");
		      String url = "jdbc:mimer://localhost/testdb2";
//		      String url = "jdbc:mimer://localhost/micro";
//		      Connection con = 
//		          DriverManager.getConnection(url, "SYSADM", "123456");
		      Connection con = 
		          DriverManager.getConnection(url, "MIMER_STORE", "GoodiesRUs");
		      Statement stmt = con.createStatement();
		      
		      String sql = "select TABLE_SCHEMA,TABLE_NAME,TABLE_TYPE from INFORMATION_SCHEMA.TABLES";
		      ResultSet rs = stmt.executeQuery(sql);
		      while (rs.next()) {
		        String schema = rs.getString(1);
		        String name = rs.getString(2);
		        String type = rs.getString(3);
		        System.out.println(schema+"  "+name+"  "+type);
		      }
		      rs.close();
		      stmt.close();
		      con.close();
		    } catch (SQLException e) {
		      System.out.println("SQLException!");
		      while (e != null) {
		        System.out.println("SQLState  : "+e.getSQLState());
		        System.out.println("Message   : "+e.getMessage());
		        System.out.println("ErrorCode : "+e.getErrorCode());
		        e = e.getNextException();
		        System.out.println("");
		      }
		    } catch (Exception e) {
		      System.out.println("Other Exception");
		      e.printStackTrace();
		    }
		
	}
}

