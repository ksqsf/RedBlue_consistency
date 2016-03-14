package applications.microbenchmark.util;
import util.Debug;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultPrint {
	
	public static void microResultsetPrint(ResultSet rq) throws SQLException{
		   int count = 0;
		   while (rq.next ())
		   {
			   int a = rq.getInt("a");
			   int b = rq.getInt("b");
			   int c = rq.getInt("c");
			   int d = rq.getInt("d");
			   String e = rq.getString("e");
		      Debug.println("a = " + a + ", b = " + b + ", c = " + c + ", d = " + d + ", e = " + e );
		      ++count;
		   }
		  Debug.println (count + " rows were retrieved");
	}

}
