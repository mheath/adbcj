package edu.byu.cs.adbcj;

import java.util.Properties;

public interface ConnectionFactory {

	DbFuture<Connection> connect(String url, String username, String password, Properties properties) throws DbException;

}
