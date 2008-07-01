import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.adbcj.*;
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

		final ResultSet rs = connection.executeQuery("SELECT * FROM large").get();
		for (Row row : rs) {
			System.out.println(row.get(0) + " " + row.get(1) + " " + row.get(2));
		}
		cm.close(true).get();
	}

}
