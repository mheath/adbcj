package org.adbcj.perf;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 *
 */
public class JDBCQueryExperiment extends AbstractJDBCExperiment {

	private final String query;
	private final int count;
	private Connection connection;

	public JDBCQueryExperiment(Configuration configuration, String host, String query, int count) {
		super(configuration, host);
		this.query = query;
		this.count = count;
	}

	public void init() throws Exception {
		connection = connect();
	}

	public void execute() throws Exception {
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
