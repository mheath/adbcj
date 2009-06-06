import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSession;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.adbcj.Value;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");

		final boolean[] callbacks = {false, false};
		final CountDownLatch latch = new CountDownLatch(2);

		DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> future) throws Exception {
				// Indicate that callback has been invoked
				callbacks[0] = true;
				latch.countDown();
			}
		});
		Connection connection = connectFuture.get(5, TimeUnit.SECONDS);
		assertTrue(!connection.isClosed());
		DbFuture<Void> closeFuture = connection.close(true).addListener(new DbListener<Void>() {
			public void onCompletion(DbFuture<Void> future) throws Exception {
				// Indicate that callback has been invoked
				callbacks[1] = true;
				latch.countDown();
			}
		});
		closeFuture.get(5, TimeUnit.SECONDS);
		assertTrue(connection.isClosed());
		latch.await(1, TimeUnit.SECONDS);
		assertTrue(callbacks[0], "Callback on connection future was not invoked");
		assertTrue(callbacks[1], "Callback on close future was not invoked");

		connectionManager.close(true);
	}

}
