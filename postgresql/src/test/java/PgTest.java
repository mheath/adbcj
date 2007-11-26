import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.ConnectionManagerProvider;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.postgresql.Adbcj;


public class PgTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws DbException 
	 */
	public static void main(String[] args) throws DbException, InterruptedException {
		Adbcj.init();
		
		ConnectionManager cm = ConnectionManagerProvider.createConnectionManager("adbcj:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		Connection connection = cm.connect().get();
		
		connection.executeUpdate("UPDATE locks SET name='dork' WHERE name='wont find'").get();
		
		cm.close(true).get();
		
		cm.close(true).get();
		System.out.println("Connection Manager Closed");
	}

}
