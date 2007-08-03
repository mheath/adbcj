package net.toadhead.adbcj.jdbc;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerFactory;
import edu.byu.cs.adbcj.ConnectionManagerProducer;
import edu.byu.cs.adbcj.DbException;

public class JdbcConnectionManagerProducer implements ConnectionManagerProducer {

	private static final String PROTOCOL = "jdbc";

	static {
		ConnectionManagerFactory.registerConnectionManagerProducer(PROTOCOL,
				new JdbcConnectionManagerProducer());

	}

	public ConnectionManager createConnectionManager(String url,
			String username, String password, ExecutorService executorService,
			Properties properties) throws DbException {
		try {
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			String jdbcUrl = uri.toString();

			return new JdbcConnectionManager(jdbcUrl, username, password, executorService, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

}
