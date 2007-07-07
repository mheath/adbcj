package edu.byu.cs.adbcj;

import java.util.Properties;

public interface ConnectionManagerProducer {

	ConnectionManager createConnectionManager(String url, String username, String password, Properties properties) throws DbException;

}
