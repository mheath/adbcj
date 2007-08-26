-- This script must be run by a MySQL user with admin privileges

GRANT SELECT, INSERT, DELETE, UPDATE ON adbcjtck.* TO adbcjtck IDENTIFIED BY 'adbcjtck';

DROP DATABASE IF EXISTS adbcjtck;
CREATE DATABASE adbcjtck;
