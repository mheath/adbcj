package org.safehaus.adbcj.postgresql;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.ConnectionManagerProducer;
import edu.byu.cs.adbcj.DbException;

public class PgConnectionManagerProducer implements ConnectionManagerProducer {

	private static final String PROTOCOL = "postgresql";

	static {
		ConnectionManagerProvider.registerConnectionManagerProducer(PROTOCOL,
				new PgConnectionManagerProducer());

	}

	public ConnectionManager createConnectionManager(String url,
			String username, String password, ExecutorService executorService,
			Properties properties) throws DbException {
		return null;
	}

}
