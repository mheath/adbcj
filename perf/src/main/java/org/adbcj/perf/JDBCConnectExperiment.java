package org.adbcj.perf;

import java.sql.SQLException;

/**
 *
 */
public class JDBCConnectExperiment extends AbstractJDBCExperiment {

	private final int count;

	public JDBCConnectExperiment(Configuration configuration, String host, int count) {
		super(configuration, host);
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
