package edu.rice.rubis.servlets;

import java.util.concurrent.atomic.AtomicInteger;

/** This class contains the configuration for the servlets
 * like the path of HTML files, etc ...
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public class Config
{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
/**
   * Creates a new <code>Config</code> instance.
   *
   */
  Config()
  {
  }
  
//  public static final int AboutMePoolSize = 10;
//  public static final int BrowseCategoriesPoolSize = 6;
//  public static final int BrowseRegionsPoolSize = 6;
//  public static final int BuyNowPoolSize = 4;
//  public static final int PutBidPoolSize = 8;
//  public static final int PutCommentPoolSize = 2;
//  public static final int RegisterItemPoolSize = 2;
//  public static final int RegisterUserPoolSize = 2;
//  public static final int SearchItemsByCategoryPoolSize = 15;
//  public static final int SearchItemsByRegionPoolSize = 20;
//  public static final int StoreBidPoolSize = 8;
//  public static final int StoreBuyNowPoolSize = 4;
//  public static final int StoreCommentPoolSize = 2;
//  public static final int ViewBidHistoryPoolSize = 4;
//  public static final int ViewItemPoolSize = 20;
//  public static final int ViewUserInfoPoolSize = 4;
  
  //the following variables are initialized in AuctionManager, not here
  public static String HTMLFilesPath =    "rubis_servlets"; 
  public static String DatabaseProperties =  "rubis_servlets/WEB-INF/classes/mysql.properties"; 
  public static AtomicInteger UserIDFactory=new AtomicInteger(0);
  public static AtomicInteger ItemIDFactory=new AtomicInteger(0);
  public static AtomicInteger BidIDFactory=new AtomicInteger(0);
  public static AtomicInteger CommentIDFactory=new AtomicInteger(0);
  public static AtomicInteger BuyNowIDFactory=new AtomicInteger(0);
  public static  int TotalProxies = 1;
  public static  int MaxProxyPerDatacenter= 1;
  public static  int TotalDatacenters = 1;
  public static  int DatacenterID = 0;
  public static  int ProxyID = 0;
  public static  int DatabasePool = 100;
  
  
  

}

