package org.adbcj.perf;

import org.adbcj.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Random;

/**
 *
 */
public class AdbcjUpdateExperiment extends AbstractAdbcjExperiment {
	private final int count;
	private final String template;
	private final Random rand = new Random();

	public AdbcjUpdateExperiment(Configuration configuration, String template, int count, String... hosts) {
		super(configuration, hosts);
		this.template = template;
		this.count = count;
	}

	@Override
	public void init() throws Exception {
		super.init();
		for (ConnectionManager connectionManager : getConnectionManagers()) {
		Connection connection = connectionManager.connect().get();
			connection.executeUpdate("DELETE FROM updates").get();
			connection.executeUpdate("INSERT INTO updates (id) VALUES (1)").get();
			connection.close(true);
		}
	}

	public void execute() throws Exception {
		for (ConnectionManager connectionManager : getConnectionManagers()) {
			final Connection connection = connectionManager.connect().get();
			final CountDownLatch latch = new CountDownLatch(count);
			final DbListener<Result> listener = new DbListener<Result>() {
				public void onCompletion(DbFuture<Result> resultSetDbFuture) throws Exception {
					latch.countDown();
					resultSetDbFuture.get();
				}
			};

			for (int i = 0; i < count; i++) {
				final String sql = String.format(template, rand.nextInt());
				connection.executeUpdate(sql).addListener(listener);
			}
			if (!latch.await(1, TimeUnit.MINUTES)) {
				throw new RuntimeException("Timed out");
			}
		}

	}
}
