package org.adbcj.perf;

import org.adbcj.DbListener;
import org.adbcj.DbFuture;
import org.adbcj.Connection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AdbcjConnectExperiment extends AbstractAdbcjExperiment {
	private final int count;

	public AdbcjConnectExperiment(Configuration configuration, String host, int count) {
		super(configuration, host);
		this.count = count;
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			getConnectionManager().connect().addListener(new DbListener<Connection>() {
				public void onCompletion(DbFuture<Connection> connectionDbFuture) throws Exception {
					latch.countDown();
				}
			});
		}
		latch.await(1, TimeUnit.MINUTES);
	}
}
