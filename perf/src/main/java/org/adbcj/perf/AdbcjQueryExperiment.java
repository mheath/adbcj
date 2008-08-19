package org.adbcj.perf;

import org.adbcj.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	protected DbSession getDbSession() {
		return getConnectionManager().connect().getUninterruptably();
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(count);
		final DbListener<ResultSet> listener = new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> resultSetDbFuture) throws Exception {
				latch.countDown();
			}
		};
		for (int i = 0; i < count; i++) {
			connection.executeQuery(query).addListener(listener);
		}
		latch.await(1, TimeUnit.MINUTES);
	}
}
