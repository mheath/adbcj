package org.adbcj.perf;

import org.adbcj.Connection;
import org.adbcj.DbListener;
import org.adbcj.DbFuture;
import org.adbcj.ConnectionManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public class AdbcjCloseExperiment extends AbstractAdbcjExperiment {

	private final int count;
	private final List<Connection> connections;

	public AdbcjCloseExperiment(Configuration configuration, int count, String... hosts) {
		super(configuration, hosts);
		this.count = count;
		connections = new ArrayList<Connection>(count * hosts.length);
	}

	@Override
	public void init() throws Exception {
		super.init();
		for (int i = 0; i < count; i++) {
			for (ConnectionManager connectionManager : getConnectionManagers()) {
				connections.add(connectionManager.connect().getUninterruptably());
			}
		}
	}

	public void execute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(connections.size());
		for (Connection connection : connections) {
			connection.close(true).addListener(new DbListener<Void>() {
				public void onCompletion(DbFuture<Void> voidDbFuture) throws Exception {
					latch.countDown();
				}
			});
		}
		if (!latch.await(1, TimeUnit.MINUTES)) {
			throw new RuntimeException("Timed out!");
		}
	}
}
