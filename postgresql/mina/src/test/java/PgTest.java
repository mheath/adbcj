import org.adbcj.DbException;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.Connection;
import org.adbcj.ResultSet;
import org.adbcj.Row;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PgTest {

	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws DbException 
	 */
	public static void main(String[] args) throws DbException, InterruptedException {
		ConnectionManager cm = ConnectionManagerProvider.createConnectionManager("adbcj:postgresql-mina://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		Connection connection = cm.connect().get();
	
		final ResultSet rs = connection.executeQuery("SELECT * FROM large").get();
		for (Row row : rs) {
			System.out.println(row.get(0) + " " + row.get(1));
		}
		cm.close(true).get();
	}

}
