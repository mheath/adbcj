package org.adbcj.perf;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 */
public class JDBCCloseExperiment extends AbstractJDBCExperiment {

	private final Connection[] connections;

	public JDBCCloseExperiment(Configuration configuration, String host, int count) {
		super(configuration, host);
		this.connections = new Connection[count];
	}

	public void init() throws SQLException {
		for (int i = 0; i < connections.length; i++) {
			connections[i] = connect();
		}
	}
	
	public void execute() throws Exception {
		for (Connection connection : connections) {
			connection.close();
		}
	}
}
