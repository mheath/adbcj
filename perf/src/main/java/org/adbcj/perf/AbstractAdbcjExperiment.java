package org.adbcj.perf;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;

import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public abstract class AbstractAdbcjExperiment extends Experiment {

	private List<ConnectionManager> connectionManagers;

	protected AbstractAdbcjExperiment(Configuration configuration, String... hosts) {
		super(configuration, hosts);
	}

	public void init() throws Exception {
		if (connectionManagers != null) {
			cleanup();
		}
		List<String> urls = getUrls();
		connectionManagers = new ArrayList<ConnectionManager>(urls.size());
		for (String url : urls) {
			connectionManagers.add(ConnectionManagerProvider.createConnectionManager(url, USER_NAME, PASSWORD));
		}
	}

	public void cleanup() throws Exception {
		for (ConnectionManager connectionManager : connectionManagers) {
			connectionManager.close(true).getUninterruptably();
		}
	}

	protected List<ConnectionManager> getConnectionManagers() {
		return connectionManagers;
	}
}
