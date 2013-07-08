package org.adbcj.mysql.netty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerFactory;
import org.adbcj.DbException;

public class MySqlConnectionManagerFactory implements ConnectionManagerFactory {

	public static final String PROTOCOL = "mysqlnetty";
	public static final int DEFAULT_PORT = 3306;

	@Override
	public ConnectionManager createConnectionManager(String url, String username, String password, Properties properties) throws DbException {
		String host = null;
		int port = 0;
		String schema = null;
		
		try {
			/*
			 * Parse URL
			 */
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			host = uri.getHost();
			port = uri.getPort();
			if (port < 0) {
				port = DEFAULT_PORT;
			}
			String path = uri.getPath().trim();
			if (path.length() == 0 || "/".equals(path)) {
				throw new DbException("You must specific a database in the URL path");
			}
			schema = path.substring(1);

			} catch (URISyntaxException e) {
			throw new DbException(e);
		}
		return new MysqlConnectionManager(host, port, username, password, schema, properties);
		
	}

	@Override
	public boolean canHandle(String protocol) {
		return PROTOCOL.equals(protocol); 
	}

}
