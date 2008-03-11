import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.ResultSet;
import org.adbcj.postgresql.Adbcj;


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

		List<DbFuture<ResultSet>> futures = new LinkedList<DbFuture<ResultSet>>();
		for (int i = 0; i < 100; i++) {
			futures.add(
					connection.executeQuery(String.format("SELECT *, %d FROM simple_values", i))
					);
		}
		
		for (DbFuture<ResultSet> future : futures) {
			try {
				future.get(5, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new AssertionError("Timed out waiting on future: " + future);
			}
		}
		
		cm.close(true).get();
		System.out.println("Connection Manager Closed");
	}

}
