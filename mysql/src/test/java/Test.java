import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.mysql.Adbcj;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Adbcj.init();
		
		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		Connection connection = connectionManager.connect().get();
		Connection lockingConnection = connectionManager.connect().get();
		
		// Get lock
		lockingConnection.beginTransaction();
		lockingConnection.executeQuery("SELECT name FROM locks WHERE name='lock' FOR UPDATE").get();
		
		// Get lock on primary connection
		connection.beginTransaction();
		connection.executeQuery("SELECT name FROM locks WHERE name='lock' FOR UPDATE");
		
		connection.executeQuery("SELECT * FROM simple_values");
		connection.executeQuery("SELECT * FROM simple_values");
		connection.executeQuery("SELECT * FROM simple_values");
		connection.executeQuery("SELECT * FROM simple_values");

		lockingConnection.commit().get();
		connectionManager.close(true);
	}

}
