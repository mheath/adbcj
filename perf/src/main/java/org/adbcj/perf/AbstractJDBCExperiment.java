package org.adbcj.perf;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides code for obtaining JDBC connections and automatically cleans up the aquired JDBC connections in
 * {@link #cleanup()}.
 */
public abstract class AbstractJDBCExperiment extends Experiment {

	private final List<Connection> managedConnections = new ArrayList<Connection>();

	protected AbstractJDBCExperiment(Configuration configuration, String... hosts) {
		super(configuration, hosts);
	}

	protected List<Connection> connect() throws SQLException {
		List<String> urls = getUrls();
		List<Connection> connections = new ArrayList<Connection>(urls.size());
		for (String url : urls) {
			Connection connection = DriverManager.getConnection(url, USER_NAME, PASSWORD);
			managedConnections.add(connection);
		}
		return connections;
	}

	/**
	 * Closes any created connections that have not already been closed.
	 *
	 * @throws SQLException  thrown if there is a problem closing one of the opened conenctions
	 */
	public void cleanup() throws SQLException {
		for (Connection connection : managedConnections) {
			if (!connection.isClosed()) {
				connection.close();
			}
		}
		managedConnections.clear();
	}

}
