package org.adbcj.perf;

import org.adbcj.Connection;
import org.adbcj.DbListener;
import org.adbcj.DbFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AdbcjCloseExperiment extends AbstractAdbcjExperiment {

	private final Connection[] connections;

	public AdbcjCloseExperiment(Configuration configuration, String host, int count) {
		super(configuration, host);
		connections = new Connection[count];
	}

	@Override
	public void init() throws Exception {
		super.init();
		for (int i = 0; i < connections.length; i++) {
			connections[i] = getConnectionManager().connect().getUninterruptably();
		}
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(connections.length);
		for (Connection connection : connections) {
			connection.close(true).addListener(new DbListener<Void>() {
				public void onCompletion(DbFuture<Void> voidDbFuture) throws Exception {
					latch.countDown();
				}
			});
		}
		latch.await(1, TimeUnit.MINUTES);
	}
}
