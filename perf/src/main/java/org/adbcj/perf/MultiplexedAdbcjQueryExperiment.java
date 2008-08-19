package org.adbcj.perf;

import org.adbcj.DbSessionPool;
import org.adbcj.DbSession;

/**
 *
 */
public class MultiplexedAdbcjQueryExperiment extends AdbcjQueryExperiment {

	private final int connectionCount;

	public MultiplexedAdbcjQueryExperiment(Configuration configuration, String host, String query, int count, int connectionCount) {
		super(configuration, host, query, count);
		this.connectionCount = connectionCount;
	}

	@Override
	protected DbSession getDbSession() {
		DbSessionPool pool = new DbSessionPool();
		pool.addConnectionManager(getConnectionManager(), connectionCount);
		// Give the connection pool a chance to create connections
		return pool.connect().getUninterruptably();
	}
}
