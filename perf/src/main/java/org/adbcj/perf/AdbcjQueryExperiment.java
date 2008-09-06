package org.adbcj.perf;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSession;
import org.adbcj.ResultSet;

/**
 *
 */
public class AdbcjQueryExperiment extends AbstractAdbcjExperiment {

	private final String query;
	private final int count;

	private DbSession connection;

	public AdbcjQueryExperiment(Configuration configuration, String host, String query, int count) {
		super(configuration, host);
		this.query = query;
		this.count = count;
	}

	@Override
	public void init() throws Exception {
		super.init();
		getConnectionManager().setPipeliningEnabled(getConfiguration().isPipelined());
		connection = getDbSession();
	}

	@Override
	public void cleanup() throws Exception {
		connection.close(true).get();
		super.cleanup();
	}
	protected DbSession getDbSession() {
		return getConnectionManager().connect().getUninterruptably();
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
			connection.executeQuery(query).addListener(listener);
		}
		if (!latch.await(10, TimeUnit.MINUTES)) {
			throw new RuntimeException("Timed out!");
		}
	}
}
