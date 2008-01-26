package org.adbcj.tck;

import java.util.concurrent.Executors;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.testng.annotations.DataProvider;

public class ConnectionManagerDataProvider {

	static {
		try {
			// Make sure static portions of each of these classes gets invoked
			org.adbcj.jdbc.Adbcj.init();
			org.adbcj.mysql.Adbcj.init();
			org.adbcj.postgresql.Adbcj.init();
			
			// Load the needed JDBC drivers
			Class.forName("com.mysql.jdbc.Driver");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final Object[][] TEST_CONNECTION_DATA = {
		{"adbcj:jdbc:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:jdbc:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
	};
	
	@DataProvider(name="urlDataProvider")
	public static Object[][] urlDataProvider() {
		return TEST_CONNECTION_DATA;
	}
	
	@DataProvider(name="connectionManagerDataProvider")
	public static synchronized Object[][] connectionManagerProvider() {
		ConnectionManager[][] connectionManager = new ConnectionManager[TEST_CONNECTION_DATA.length][1];
		for (int i = 0 ; i < TEST_CONNECTION_DATA.length; i++) {
			Object[] connectParams = TEST_CONNECTION_DATA[i];
			connectionManager[i] = new ConnectionManager[] {ConnectionManagerProvider.createConnectionManager(
					connectParams[0].toString(),
					connectParams[1].toString(),
					connectParams[2].toString())
			};
		}
		return connectionManager;
	}

}
