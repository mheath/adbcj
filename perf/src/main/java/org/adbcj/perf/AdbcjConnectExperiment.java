package org.adbcj.perf;

import org.adbcj.DbListener;
import org.adbcj.DbFuture;
import org.adbcj.Connection;
import org.adbcj.ConnectionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AdbcjConnectExperiment extends AbstractAdbcjExperiment {
	private final int count;

	public AdbcjConnectExperiment(Configuration configuration, int count, String... hosts) {
		super(configuration, hosts);
		this.count = count;
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			for (ConnectionManager connectionManager: getConnectionManagers()) {
				connectionManager.connect().addListener(new DbListener<Connection>() {
					public void onCompletion(DbFuture<Connection> connectionDbFuture) throws Exception {
						latch.countDown();
					}
				});
			}
		}
		latch.await(1, TimeUnit.MINUTES);
	}
}
