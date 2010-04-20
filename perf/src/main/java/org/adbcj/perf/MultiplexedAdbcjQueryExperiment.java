package org.adbcj.perf;

import org.adbcj.DbSessionPool;
import org.adbcj.DbSession;
import org.adbcj.ConnectionManager;

import java.util.List;
import java.util.Collections;

/**
 *
 */
public class MultiplexedAdbcjQueryExperiment extends AdbcjQueryExperiment {

	private final int connectionCount;

	public MultiplexedAdbcjQueryExperiment(Configuration configuration, String query, int count, int connectionCount, String... hosts) {
		super(configuration, query, count, hosts);
		this.connectionCount = connectionCount;
	}

	@Override
	protected List<DbSession> getDbSessions() {
		DbSessionPool pool = new DbSessionPool();
		for (ConnectionManager connectionManager : getConnectionManagers()) {
			pool.addConnectionManager(connectionManager, connectionCount);
		}
		// Give the connection pool a chance to create connections
		return Collections.singletonList(pool.connect().getUninterruptably());
	}
}
