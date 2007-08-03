package edu.byu.cs.adbcj;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

public interface ConnectionManagerProducer {

	ConnectionManager createConnectionManager(String url, String username, String password, ExecutorService executorService, Properties properties) throws DbException;

}
