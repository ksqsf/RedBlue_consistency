-- drop database rubis;

-- create db
create database if not exists rubis;
connect rubis;

-- create user
-- create user if not exists 'sa'@'localhost';
-- alter user 'sa'@'localhost' identified by '101010';

-- grant privileges
grant all on rubis.* to 'sa'@'%' identified by '101010';
grant all on rubis.* to 'sa'@'localhost' identified by '101010';

-- create tables
CREATE TABLE categories (
   id INTEGER UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT, /*this table is not changed over the experiment, keep it */
   name VARCHAR(50),
   PRIMARY KEY(id)
);

CREATE TABLE regions (
   id INTEGER UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT, /*this table is not changed over the experiment, keep it */
   name VARCHAR(25),
   PRIMARY KEY(id)
);

CREATE TABLE users (
   id            INTEGER UNSIGNED NOT NULL UNIQUE /*AUTO_INCREMENT - txmud doesnt allow autoincrement fields*/,
   firstname     VARCHAR(20),
   lastname      VARCHAR(20),
   nickname      VARCHAR(20) NOT NULL UNIQUE,
   password      VARCHAR(20) NOT NULL,
   email         VARCHAR(50) NOT NULL,
   rating        INTEGER,
   balance       FLOAT,
   creation_date TIMESTAMP, /*DATETIME, - doesnt carry information about timezone and may affect remote datacenters */ 
   region        INTEGER UNSIGNED NOT NULL,
   PRIMARY KEY(id),
   INDEX auth (nickname,password),
   INDEX region_id (region)
);

CREATE TABLE items (
   id            INTEGER UNSIGNED NOT NULL UNIQUE /*AUTO_INCREMENT - txmud doesnt allow autoincrement fields*/,
   name          VARCHAR(100),
   description   TEXT,
   initial_price FLOAT UNSIGNED NOT NULL,
   quantity      INTEGER UNSIGNED NOT NULL,
   reserve_price FLOAT UNSIGNED DEFAULT 0,
   buy_now       FLOAT UNSIGNED DEFAULT 0,
   nb_of_bids    INTEGER UNSIGNED DEFAULT 0,
   max_bid       FLOAT UNSIGNED DEFAULT 0,
   start_date    TIMESTAMP,
   end_date      TIMESTAMP,
   seller        INTEGER UNSIGNED NOT NULL,
   category      INTEGER UNSIGNED NOT NULL,
   winnerid		  INTEGER UNSIGNED DEFAULT NULL, /*define the winner*/
   PRIMARY KEY(id),
   INDEX seller_id (seller),
   INDEX category_id (category)
);
/*
CREATE TABLE old_items (
   id            INTEGER UNSIGNED NOT NULL UNIQUE,
   name          VARCHAR(100),
   description   TEXT,
   initial_price FLOAT UNSIGNED NOT NULL,
   quantity      INTEGER UNSIGNED NOT NULL,
   reserve_price FLOAT UNSIGNED DEFAULT 0,
   buy_now       FLOAT UNSIGNED DEFAULT 0,
   nb_of_bids    INTEGER UNSIGNED DEFAULT 0,
   max_bid       FLOAT UNSIGNED DEFAULT 0,
   start_date    DATETIME,
   end_date      DATETIME,
   seller        INTEGER UNSIGNED NOT NULL,
   category      INTEGER UNSIGNED NOT NULL,
   PRIMARY KEY(id),
   INDEX seller_id (seller),
   INDEX category_id (category)
);
*/
CREATE TABLE bids (
   id      INTEGER UNSIGNED NOT NULL UNIQUE /*AUTO_INCREMENT - txmud doesnt allow autoincrement fields*/,
   user_id INTEGER UNSIGNED NOT NULL,
   item_id INTEGER UNSIGNED NOT NULL,
   qty     INTEGER UNSIGNED NOT NULL,
   bid     FLOAT UNSIGNED NOT NULL,
   max_bid FLOAT UNSIGNED NOT NULL,
   date    TIMESTAMP, /*DATETIME, - doesnt carry information about timezone and may affect remote datacenters */
   PRIMARY KEY(id),
   INDEX item (item_id),
   INDEX user (user_id)
);

CREATE TABLE comments (
   id           INTEGER UNSIGNED NOT NULL UNIQUE /*AUTO_INCREMENT - txmud doesnt allow autoincrement fields*/,
   from_user_id INTEGER UNSIGNED NOT NULL,
   to_user_id   INTEGER UNSIGNED NOT NULL,
   item_id      INTEGER UNSIGNED NOT NULL,
   rating       INTEGER,
   date         TIMESTAMP, /*DATETIME, - doesnt carry information about timezone and may affect remote datacenters */
   comment      TEXT,
   PRIMARY KEY(id),
   INDEX from_user (from_user_id),
   INDEX to_user (to_user_id),
   INDEX item (item_id)
);

CREATE TABLE buy_now (
   id       INTEGER UNSIGNED NOT NULL UNIQUE /*AUTO_INCREMENT - txmud doesnt allow autoincrement fields*/,
   buyer_id INTEGER UNSIGNED NOT NULL,
   item_id  INTEGER UNSIGNED NOT NULL,
   qty      INTEGER UNSIGNED NOT NULL,
   date     TIMESTAMP, /*DATETIME, - doesnt carry information about timezone and may affect remote datacenters */
   PRIMARY KEY(id),
   INDEX buyer (buyer_id),
   INDEX item (item_id)
);
/*
CREATE TABLE ids (
   id        INTEGER UNSIGNED NOT NULL UNIQUE,
   category  INTEGER UNSIGNED NOT NULL,
   region    INTEGER UNSIGNED NOT NULL,
   users     INTEGER UNSIGNED NOT NULL,
   item      INTEGER UNSIGNED NOT NULL,
   comment   INTEGER UNSIGNED NOT NULL,
   bid       INTEGER UNSIGNED NOT NULL,
   buyNow    INTEGER UNSIGNED NOT NULL,
   PRIMARY KEY(id)
);
*/

-- an additional table from rubis_vasco.sql
CREATE TABLE winners (
   winner_id INTEGER UNSIGNED NOT NULL,
   item_id  INTEGER UNSIGNED NOT NULL,
   bid     FLOAT UNSIGNED NOT NULL,
   INDEX winner (winner_id),
   INDEX item (item_id)
);
