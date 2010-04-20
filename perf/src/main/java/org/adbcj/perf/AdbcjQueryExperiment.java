package org.adbcj.perf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSession;
import org.adbcj.ResultSet;
import org.adbcj.ConnectionManager;

/**
 *
 */
public class AdbcjQueryExperiment extends AbstractAdbcjExperiment {

	private final String query;
	private final int count;

	private List<DbSession> connections;

	public AdbcjQueryExperiment(Configuration configuration, String query, int count, String... hosts) {
		super(configuration, hosts);
		this.query = query;
		this.count = count;
	}

	@Override
	public void init() throws Exception {
		super.init();
		for (ConnectionManager connectionManager : getConnectionManagers()) {
			connectionManager.setPipeliningEnabled(getConfiguration().isPipelined());
		}
		connections = getDbSessions();
	}

	@Override
	public void cleanup() throws Exception {
		for (DbSession connection : connections) {
			connection.close(true).get();
		}
		connections.clear();
		super.cleanup();
	}
	protected List<DbSession> getDbSessions() {
		List<ConnectionManager> connectionManagers = getConnectionManagers();
		List<DbSession> sessions = new ArrayList<DbSession>(connectionManagers.size());
		for (ConnectionManager connectionManager : connectionManagers) {
			sessions.add(connectionManager.connect().getUninterruptably());
		}
		return sessions;
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(count);
		final DbListener<ResultSet> listener = new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> resultSetDbFuture) throws Exception {
				latch.countDown();
				resultSetDbFuture.get();
			}
		};
		for (int i = 0; i < count; i++) {
			for (DbSession connection : connections) {
				connection.executeQuery(query).addListener(listener);
			}
		}
		if (!latch.await(10, TimeUnit.MINUTES)) {
			throw new RuntimeException("Timed out!");
		}
	}
}
