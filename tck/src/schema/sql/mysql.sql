-- This script must be run by a MySQL user with admin privileges

GRANT SELECT, INSERT, DELETE, UPDATE ON adbcjtck.* TO adbcjtck@localhost IDENTIFIED BY 'adbcjtck';

DROP DATABASE IF EXISTS adbcjtck;
CREATE DATABASE adbcjtck;

USE adbcjtck;

DROP TABLE IF EXISTS simple_values;
CREATE TABLE simple_values (
  int_val int,
  str_val varchar(255)
);

INSERT INTO simple_values (int_val, str_val) values (null, null);
INSERT INTO simple_values (int_val, str_val) values (0, 'Zero');
INSERT INTO simple_values (int_val, str_val) values (1, 'One');
INSERT INTO simple_values (int_val, str_val) values (2, 'Two');
INSERT INTO simple_values (int_val, str_val) values (3, 'Three');
INSERT INTO simple_values (int_val, str_val) values (4, 'Four');

DROP TABLE IF EXISTS updates;
CREATE TABLE updates (id int) type=InnoDB;

DROP TABLE IF EXISTS locks;
CREATE TABLE locks (name varchar(255) primary key not null) type=InnoDB;
INSERT INTO locks(name) VALUES ('lock');
