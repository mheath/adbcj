package org.adbcj.perf;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public class JDBCCloseExperiment extends AbstractJDBCExperiment {

	private final int count;
	private final List<Connection> connections;

	public JDBCCloseExperiment(Configuration configuration, int count, String... hosts) {
		super(configuration, hosts);
		this.count = count;
		this.connections = new ArrayList<Connection>(count * hosts.length);
	}

	public void init() throws SQLException {
		for (int i = 0; i < count; i++) {
			for (Connection connection : connect()) {
				connections.add(connection);
			}
		}
	}
	
	public void execute() throws Exception {
		for (Connection connection : connections) {
			connection.close();
		}
	}
}
