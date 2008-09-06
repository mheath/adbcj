package org.adbcj.mysql.netty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerFactory;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;

public class MySqlConnectionManagerFactory implements ConnectionManagerFactory {

	public static final String PROTOCOL = "mysqlnetty";
	public static final int DEFAULT_PORT = 3306;

	static void register() {
		MySqlConnectionManagerFactory mysqlConnectionManagerFactory = new MySqlConnectionManagerFactory();
		ConnectionManagerProvider.registerConnectionManagerFactory(PROTOCOL, mysqlConnectionManagerFactory);
	}

	private MySqlConnectionManagerFactory() {
	}

	@Override
	public ConnectionManager createConnectionManager(String url, String username, String password, Properties properties) throws DbException {
		try {
			/*
			 * Parse URL
			 */
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			String host = uri.getHost();
			int port = uri.getPort();
			if (port < 0) {
				port = DEFAULT_PORT;
			}
			String path = uri.getPath().trim();
			if (path.length() == 0 || "/".equals(path)) {
				throw new DbException("You must specific a database in the URL path");
			}
			String schema = path.substring(1);

			return new MysqlConnectionManager(host, port, username, password, schema, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

}
