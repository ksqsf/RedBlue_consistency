# MySQL dump 8.16
#
# Host: localhost    Database: rubis
#--------------------------------------------------------
# Server version	3.23.43-max

#
# Table structure for table 'bids'
#

CREATE TABLE bids (
  id int(10) unsigned NOT NULL auto_increment,
  user_id int(10) unsigned NOT NULL default '0',
  item_id int(10) unsigned NOT NULL default '0',
  qty int(10) unsigned NOT NULL default '0',
  bid float unsigned NOT NULL default '0',
  max_bid float unsigned NOT NULL default '0',
  date datetime default NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY id (id),
  KEY item (item_id),
  KEY user (user_id)
) TYPE=MyISAM;

#
# Dumping data for table 'bids'
#


#
# Table structure for table 'buy_now'
#

CREATE TABLE buy_now (
  id int(10) unsigned NOT NULL auto_increment,
  buyer_id int(10) unsigned NOT NULL default '0',
  item_id int(10) unsigned NOT NULL default '0',
  qty int(10) unsigned NOT NULL default '0',
  date datetime default NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY id (id),
  KEY buyer (buyer_id),
  KEY item (item_id)
) TYPE=MyISAM;

#
# Dumping data for table 'buy_now'
#


#
# Table structure for table 'categories'
#

CREATE TABLE categories (
  id int(10) unsigned NOT NULL auto_increment,
  name varchar(50) default NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY id (id)
) TYPE=MyISAM;

#
# Dumping data for table 'categories'
#


#
# Table structure for table 'comments'
#

CREATE TABLE comments (
  id int(10) unsigned NOT NULL auto_increment,
  from_user_id int(10) unsigned NOT NULL default '0',
  to_user_id int(10) unsigned NOT NULL default '0',
  item_id int(10) unsigned NOT NULL default '0',
  rating int(11) default NULL,
  date datetime default NULL,
  comment text,
  PRIMARY KEY  (id),
  UNIQUE KEY id (id),
  KEY from_user (from_user_id),
  KEY to_user (to_user_id),
  KEY item (item_id)
) TYPE=MyISAM;

#
# Dumping data for table 'comments'
#


#
# Table structure for table 'ids'
#

CREATE TABLE ids (
  id int(10) unsigned NOT NULL default '0',
  category int(10) unsigned NOT NULL default '0',
  region int(10) unsigned NOT NULL default '0',
  users int(10) unsigned NOT NULL default '0',
  item int(10) unsigned NOT NULL default '0',
  comment int(10) unsigned NOT NULL default '0',
  bid int(10) unsigned NOT NULL default '0',
  buyNow int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (id),
  UNIQUE KEY id (id)
) TYPE=MyISAM;

#
# Dumping data for table 'ids'
#


#
# Table structure for table 'items'
#

CREATE TABLE items (
  id int(10) unsigned NOT NULL auto_increment,
  name varchar(100) default NULL,
  description text,
  initial_price float unsigned NOT NULL default '0',
  quantity int(10) unsigned NOT NULL default '0',
  reserve_price float unsigned default '0',
  buy_now float unsigned default '0',
  nb_of_bids int(10) unsigned default '0',
  max_bid float unsigned default '0',
  start_date datetime default NULL,
  end_date datetime default NULL,
  seller int(10) unsigned NOT NULL default '0',
  category int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (id),
  UNIQUE KEY id (id),
  KEY seller_id (seller),
  KEY category_id (category),
  KEY bday (start_date),
  KEY eday (end_date)
) TYPE=MyISAM;

#
# Dumping data for table 'items'
#


#
# Table structure for table 'old_items'
#

CREATE TABLE old_items (
  id int(10) unsigned NOT NULL default '0',
  name varchar(100) default NULL,
  description text,
  initial_price float unsigned NOT NULL default '0',
  quantity int(10) unsigned NOT NULL default '0',
  reserve_price float unsigned default '0',
  buy_now float unsigned default '0',
  nb_of_bids int(10) unsigned default '0',
  max_bid float unsigned default '0',
  start_date datetime default NULL,
  end_date datetime default NULL,
  seller int(10) unsigned NOT NULL default '0',
  category int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (id),
  UNIQUE KEY id (id),
  KEY seller_id (seller),
  KEY category_id (category)
) TYPE=MyISAM;

#
# Dumping data for table 'old_items'
#


#
# Table structure for table 'regions'
#

CREATE TABLE regions (
  id int(10) unsigned NOT NULL auto_increment,
  name varchar(25) default NULL,
  PRIMARY KEY  (id),
  UNIQUE KEY id (id)
) TYPE=MyISAM;

#
# Dumping data for table 'regions'
#


#
# Table structure for table 'users'
#

CREATE TABLE users (
  id int(10) unsigned NOT NULL auto_increment,
  firstname varchar(20) default NULL,
  lastname varchar(20) default NULL,
  nickname varchar(20) NOT NULL default '',
  password varchar(20) NOT NULL default '',
  email varchar(50) NOT NULL default '',
  rating int(11) default NULL,
  balance float default NULL,
  creation_date datetime default NULL,
  region int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (id),
  UNIQUE KEY nickname (nickname),
  UNIQUE KEY id (id),
  KEY auth (nickname,password),
  KEY region_id (region)
) TYPE=MyISAM;

#
# Dumping data for table 'users'
#


