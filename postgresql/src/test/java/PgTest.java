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
		connection.close(true).get();
	}

}
