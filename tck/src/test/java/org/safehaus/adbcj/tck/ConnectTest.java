package org.safehaus.adbcj.tck;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;

// TODO Test non-immediate close and make sure any pending queries get called
// TODO Test immediate close and make sure pending queries get canceled
// TODO Test connecting to bogus database to make sure we can cancel connect future
// TODO Test callbacks

public class ConnectTest extends ConnectionManagerDataProvider {

	//@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testConnectBlockClose(ConnectionManager connectionManager) throws DbException, InterruptedException {
		final boolean[] callbacks = {false, false};
		final CountDownLatch latch = new CountDownLatch(2);
		
		DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> future) throws Exception {
				// Indicate that callback has been invoked
				callbacks[0] = true;
				latch.countDown();
			}
		});
		Connection connection = connectFuture.get();
		Assert.assertTrue(!connection.isClosed());
		DbFuture<Void> closeFuture = connection.close(true).addListener(new DbListener<Void>() {
			public void onCompletion(DbFuture<Void> future) throws Exception {
				// Indicate that callback has been invoked
				callbacks[1] = true;
				latch.countDown();
			}
		});
		closeFuture.get();
		Assert.assertTrue(connection.isClosed());
		latch.await(1, TimeUnit.SECONDS);
		Assert.assertTrue(callbacks[0], "Callback on connection future was not invoked");
		Assert.assertTrue(callbacks[1], "Callback on close future was not invoked");
	}

	@Test(dataProvider="urlDataProvider")
	public void testConnectBadCredentials(String url, String user, String password) throws InterruptedException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			final boolean[] callbacks = {false};
			final CountDownLatch latch = new CountDownLatch(2);

			ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, "__BADPASSWORD__", executorService);
			DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
				public void onCompletion(DbFuture<Connection> future) throws Exception {
					callbacks[0] = true;
					latch.countDown();
				}
			});
			try {
				connectFuture.get();
				Assert.fail("Connect should have failed because of bad credentials");
			} catch (DbException e) {
				Assert.assertTrue(connectFuture.isDone(), "Connect future should be marked done even though it failed");
				Assert.assertTrue(!connectFuture.isCancelled(), "Connect future should not be marked as cancelled");
			}
			latch.await(1, TimeUnit.SECONDS);
			Assert.assertTrue(callbacks[0], "Connect future callback was not invoked when connect failed");
		} finally {
			executorService.shutdown();
		}
	}
	
}
