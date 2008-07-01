package org.adbcj.perf;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;

/**
 *
 */
public abstract class AbstractAdbcjExperiment extends Experiment {

	private ConnectionManager connectionManager;

	protected AbstractAdbcjExperiment(Configuration configuration, String host) {
		super(configuration, host);
	}

	public void init() throws Exception {
		if (connectionManager != null) {
			connectionManager.close(true).getUninterruptably();
		}
		connectionManager = ConnectionManagerProvider.createConnectionManager(getUrl(), USER_NAME, PASSWORD);
	}

	public void cleanup() throws Exception {
		connectionManager.close(true);
	}

	protected ConnectionManager getConnectionManager() {
		return connectionManager;
	}
}
