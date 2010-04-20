package org.adbcj.perf;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class JDBCQueryExperiment extends AbstractJDBCExperiment {

	private final String query;
	private final int count;
	private List<Connection> connections;

	public JDBCQueryExperiment(Configuration configuration, String query, int count, String... hosts) {
		super(configuration, hosts);
		this.query = query;
		this.count = count;
	}

	public void init() throws Exception {
		connections = connect();
	}

	@Override
	public void cleanup() throws SQLException {
		super.cleanup();
		connections = null;
	}

	public void execute() throws Exception {
		for (Connection connection : connections) {
			for (int i = 0; i < count; i++) {
				final Statement statement = connection.createStatement();
				final ResultSet resultSet = statement.executeQuery(query);
				while (resultSet.next()) {
					resultSet.getObject(1);
				}
				resultSet.close();
				statement.close();
			}
		}
	}
}
