package org.adbcj.postgresql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerFactory;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;

public class PgConnectionManagerFactory implements ConnectionManagerFactory {

	public static final String PROTOCOL = "postgresql";
	public static final int DEFAULT_PORT = 5432; 

	static void register() {
		ConnectionManagerProvider.registerConnectionManagerFactory(PROTOCOL, new PgConnectionManagerFactory());

	}
	
	private PgConnectionManagerFactory() {
		// Empty private constructor
	}

	public ConnectionManager createConnectionManager(String url, String username, String password, ExecutorService executorService, Properties properties) throws DbException {
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
			String schema = uri.getPath().substring(1);

			return new PgConnectionManager(host, port, username, password, schema, executorService, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

}
