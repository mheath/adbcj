package org.safehaus.adbcj.tck;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.ConnectionManagerProvider;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbListener;
import org.safehaus.adbcj.DbSessionFuture;
import org.safehaus.adbcj.ResultSet;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


// TODO Test non-immediate close and make sure any pending queries get called
// TODO Test immediate close and make sure pending queries get canceled

public class ConnectTest extends ConnectionManagerDataProvider {

	private static final String UNREACHABLE_HOST = "1.0.0.1";

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testConnectImmediateClose(ConnectionManager connectionManager) throws DbException, InterruptedException {
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
		assertTrue(!connection.isClosed());
		DbFuture<Void> closeFuture = connection.close(true).addListener(new DbListener<Void>() {
			public void onCompletion(DbFuture<Void> future) throws Exception {
				// Indicate that callback has been invoked
				callbacks[1] = true;
				latch.countDown();
			}
		});
		closeFuture.get();
		assertTrue(connection.isClosed());
		latch.await(1, TimeUnit.SECONDS);
		assertTrue(callbacks[0], "Callback on connection future was not invoked");
		assertTrue(callbacks[1], "Callback on close future was not invoked");
	}

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testConnectNonImmediateClose(ConnectionManager connectionManager) throws DbException, InterruptedException {
		final boolean[] callbacks = {false};
		final CountDownLatch latch = new CountDownLatch(1);

		Connection connection = connectionManager.connect().get();
		assertTrue(!connection.isClosed());
		connection.close(true).addListener(new DbListener<Void>() {
			public void onCompletion(DbFuture<Void> future) throws Exception {
				// Indicate that close callback has been invoked
				callbacks[0] = true;
				latch.countDown();
			}
		}).get();
		assertTrue(connection.isClosed());
		latch.await(1, TimeUnit.SECONDS);
		assertTrue(callbacks[0], "Callback on close future was not invoked");
	}

	@Test(dataProvider="connectionManagerDataProvider")
	public void testCancelClose(ConnectionManager connectionManager) throws DbException, InterruptedException {
		final boolean[] closeCallback = {false, false};
		
		// This connection is used for doing a select for update lock
		Connection lockConnection = connectionManager.connect().get();
		Connection connectionToClose = connectionManager.connect().get();

		try {
			// Get lock with select for update
			lockConnection.beginTransaction();
			TestUtils.selectForUpdate(lockConnection).get();

			// Do select for update on second connection so we can close it and then cancel the close
			connectionToClose.beginTransaction();
			DbFuture<ResultSet> future = TestUtils.selectForUpdate(connectionToClose);

			DbSessionFuture<Void> closeFuture = connectionToClose.close(false).addListener(new DbListener<Void>() {
				public void onCompletion(DbFuture<Void> future) throws Exception {
					closeCallback[0] = true;
					closeCallback[1] = future.isCancelled();
				}
			});
			assertTrue(connectionToClose.isClosed(), "This connection should be flagged as closed now");
			assertTrue(closeFuture.cancel(false), "The connection close should have cancelled properly");
			assertFalse(connectionToClose.isClosed(), "This connection should not be closed because we canceled the close");

			// Release lock
			lockConnection.rollback().get();

			// Make sure closingConnection's select for update completed successfully
			future.get();
			connectionToClose.rollback().get();
		} finally {
			if (lockConnection.isInTransaction()) {
				lockConnection.rollback().get();
			}
			if (connectionToClose.isInTransaction()) {
				connectionToClose.rollback().get();
			}
			
			lockConnection.close(true);
			connectionToClose.close(true);
		}
		// Make sure the close's callback was invoked properly
		assertTrue(closeCallback[0], "The close callback was not invoked when cancelled");
		assertTrue(closeCallback[1], "The close future did not indicate the close was cancelled");
	}
	
	@Test(dataProvider="urlDataProvider", timeOut=5000)
	public void testConnectBadCredentials(String url, String user, String password) throws InterruptedException {
		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			final boolean[] callbacks = {false};
			final CountDownLatch latch = new CountDownLatch(1);

			ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, "__BADPASSWORD__", executorService);
			DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
				public void onCompletion(DbFuture<Connection> future) throws Exception {
					callbacks[0] = true;
					latch.countDown();
				}
			});
			try {
				connectFuture.get();
				fail("Connect should have failed because of bad credentials");
			} catch (DbException e) {
				assertTrue(connectFuture.isDone(), "Connect future should be marked done even though it failed");
				assertTrue(!connectFuture.isCancelled(), "Connect future should not be marked as cancelled");
			}
			assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback was not invoked in time");
			assertTrue(callbacks[0], "Connect future callback was not invoked with connect failure");
		} finally {
			executorService.shutdown();
		}
	}
	
	@Test(dataProvider="urlDataProvider", timeOut=5000)
	public void testConnectCancel(String url, String user, String password) throws Exception {
		StringBuilder urlBuilder = new StringBuilder();
		
		URI connectUrl = new URI(url);
		String scheme = connectUrl.getScheme();
		while (scheme != null) {
			urlBuilder.append(scheme).append(":");
			connectUrl = new URI(connectUrl.getSchemeSpecificPart());
			scheme = connectUrl.getScheme();
		}
		
		urlBuilder.append("//").append(UNREACHABLE_HOST);
		urlBuilder.append(connectUrl.getPath());
		
		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			final boolean[] callbacks = {false};
			final CountDownLatch latch = new CountDownLatch(1);
			
			ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(urlBuilder.toString(), "dummyuser", "dummypassword", executorService);
			DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
				public void onCompletion(DbFuture<Connection> future) throws Exception {
					callbacks[0] = true;
					latch.countDown();
				}
			});
			assertTrue(connectFuture.cancel(true), "Connection to unreachable host was not canceled");
			assertTrue(connectFuture.isCancelled());
			assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback was not invoked in time");
			assertTrue(callbacks[0], "Connect future callback was not invoked with connect cancellation");
		} finally {
			//executorService.shutdown();
		}
	}
	
}
