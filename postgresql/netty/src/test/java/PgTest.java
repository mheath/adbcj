import org.adbcj.DbException;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.Connection;
import org.adbcj.ResultSet;
import org.adbcj.Row;
import org.adbcj.DbFuture;

/**
 * @author Mike Heath
 */
public class PgTest {


	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws org.adbcj.DbException
	 */
	public static void main(String[] args) throws DbException, InterruptedException {
		ConnectionManager cm = ConnectionManagerProvider.createConnectionManager("adbcj:postgresql-netty://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		Connection connection = cm.connect().get();

		final DbFuture<ResultSet> future = connection.executeQuery("SELECT * FROM simple_values");
		final DbFuture<ResultSet> future2 = connection.executeQuery("SELECT * FROM large");
		ResultSet rs = future.get();
		ResultSet rs2 = future2.get();
		for (Row row : rs) {
			System.out.println(row.get(0) + " " + row.get(1));
		}
		for (Row row : rs2) {
			System.out.println(row.get(0) + " " + row.get(1));
		}
		connection.close(true).get();
		cm.close(true).get();
		System.out.println("Closed");
	}



}
