package edu.byu.cs.adbcj.mysql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerFactory;
import edu.byu.cs.adbcj.ConnectionManagerProducer;
import edu.byu.cs.adbcj.DbException;

public class MysqlConnectionManagerProducer implements ConnectionManagerProducer {

	public static final String PROTOCOL = "mysql";
	public static final int DEFAULT_PORT = 3306;
	
	static {
		ConnectionManagerFactory.registerConnectionManagerProducer(PROTOCOL, new MysqlConnectionManagerProducer());
	}
	
	
	private MysqlConnectionManagerProducer() {
	}
	
	public ConnectionManager createConnectionManager(String url, String username, String password, Properties properties) throws DbException {
		String host;
		int port;
		String database;
		/*
		 * Parse URL
		 */
		try {
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			host = uri.getHost();
			port = uri.getPort();
			if (port < 0) {
				port = DEFAULT_PORT;
			}
			database = uri.getPath().substring(1);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
		
		return new MysqlConnectionManager(host, port, username, password, database, properties);
	}

}
