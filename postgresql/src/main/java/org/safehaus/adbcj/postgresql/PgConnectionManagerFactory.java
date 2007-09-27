package org.safehaus.adbcj.postgresql;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.ConnectionManagerFactory;
import org.safehaus.adbcj.ConnectionManagerProvider;
import org.safehaus.adbcj.DbException;


public class PgConnectionManagerFactory implements ConnectionManagerFactory {

	private static final String PROTOCOL = "postgresql";

	static {
		ConnectionManagerProvider.registerConnectionManagerFactory(PROTOCOL,
				new PgConnectionManagerFactory());

	}

	public ConnectionManager createConnectionManager(String url,
			String username, String password, ExecutorService executorService,
			Properties properties) throws DbException {
		return null;
	}

}
