package org.adbcj.perf;

import java.sql.SQLException;

/**
 *
 */
public class JDBCConnectExperiment extends AbstractJDBCExperiment {

	private final int count;

	public JDBCConnectExperiment(Configuration configuration, int count, String... hosts) {
		super(configuration, hosts);
		this.count = count;
	}

	public void init() {
		// Nothing to initialize
	}
	public void execute() throws SQLException {
		for (int i = 0; i < count; i++) {
			connect();
		}
	}

}
