package org.safehaus.adbcj.tck;

import java.util.concurrent.Executors;

import net.toadhead.adbcj.jdbc.JdbcConnectionManagerProducer;

import org.safehaus.adbcj.postgresql.PgConnectionManagerProducer;
import org.testng.annotations.DataProvider;

import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.mysql.MysqlConnectionManagerProducer;

public class ConnectionManagerDataProvider {

	static {
		try {
			// Make sure static portions of each of these classes gets invoked  TODO: Find a better way to register drivers
			Class.forName(JdbcConnectionManagerProducer.class.getName());
			Class.forName(MysqlConnectionManagerProducer.class.getName());
			Class.forName(PgConnectionManagerProducer.class.getName());
			
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
		//{"adbcj:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
	};

	private static Object[][] CONNECTION_MANAGERS;
	
	@DataProvider(name="urlDataProvider")
	public static Object[][] urlDataProvider() {
		return TEST_CONNECTION_DATA.clone();
	}
	
	@DataProvider(name="connectionManagerDataProvider")
	public static synchronized Object[][] connectionManagerProvider() {
		if (CONNECTION_MANAGERS == null) {
			CONNECTION_MANAGERS = new Object[TEST_CONNECTION_DATA.length][1];
			for (int i = 0 ; i < TEST_CONNECTION_DATA.length; i++) {
				Object[] connectParams = TEST_CONNECTION_DATA[i];
				CONNECTION_MANAGERS[i] = new Object[] {ConnectionManagerProvider.createConnectionManager(
						connectParams[0].toString(),
						connectParams[1].toString(),
						connectParams[2].toString(),
						Executors.newCachedThreadPool())
				};
			}
		}
		return CONNECTION_MANAGERS;
	}
	
}
