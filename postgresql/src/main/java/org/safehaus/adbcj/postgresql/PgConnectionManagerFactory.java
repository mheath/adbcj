package org.safehaus.adbcj.postgresql;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.ConnectionManagerFactory;
import edu.byu.cs.adbcj.DbException;

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
