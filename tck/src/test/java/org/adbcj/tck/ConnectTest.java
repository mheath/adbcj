package org.adbcj.tck;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSessionFuture;
import org.adbcj.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ConnectTest {
	
	private final Logger logger = LoggerFactory.getLogger(ConnectTest.class);

	private static final String UNREACHABLE_HOST = "1.0.0.1";

	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="connectionManagerDataProvider", timeOut=5000)
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

	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="connectionManagerDataProvider", timeOut=5000)
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

	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testCancelClose(final ConnectionManager connectionManager) throws DbException, InterruptedException {
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
					logger.debug("testCancelClose: In close callback for connectionManager {}", connectionManager);
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
	
	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="urlDataProvider", timeOut=5000)
	public void testConnectBadCredentials(String url, String user, String password) throws InterruptedException {
		final boolean[] callbacks = {false};
		final CountDownLatch latch = new CountDownLatch(1);

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, "__BADPASSWORD__");
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
	}
	
	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="urlDataProvider", timeOut=60000)
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
		
		final boolean[] callbacks = {false};
		final CountDownLatch latch = new CountDownLatch(1);

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(urlBuilder.toString(), "dummyuser", "dummypassword");
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
	}

	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="connectionManagerDataProvider")
	public void testNonImmediateClose(final ConnectionManager connectionManager) throws InterruptedException {
		Connection connection = connectionManager.connect().get();

		List<DbSessionFuture<ResultSet>> futures = new ArrayList<DbSessionFuture<ResultSet>>();

		for (int i = 0; i < 5; i++) {
			futures.add(connection.executeQuery("SELECT * FROM simple_values"));
		}
		connection.close(false).get();
		assertTrue(connection.isClosed(), "Connection should be closed");
		for (DbSessionFuture<ResultSet> future : futures) {
			assertTrue(future.isDone(), "Future did not finish before connection was closed.");
			assertFalse(future.isCancelled(), "Future was cancelled and should have been");
		}
	}

	@Test(dataProviderClass=ConnectionManagerDataProvider.class, dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testImmediateClose(final ConnectionManager connectionManager) throws InterruptedException {
		Connection lockingConnection = connectionManager.connect().get();
		Connection connection = connectionManager.connect().get();

		lockingConnection.beginTransaction();
		TestUtils.selectForUpdate(lockingConnection).get();

		List<DbSessionFuture<ResultSet>> futures = new ArrayList<DbSessionFuture<ResultSet>>();

		connection.beginTransaction();

		for (int i = 0; i < 5; i++) {
			futures.add(TestUtils.selectForUpdate(connection));
		}

		logger.debug("Closing connection");
		connection.close(true).get();
		logger.debug("Closed");

		assertTrue(connection.isClosed(), "Connection should be closed");
		for (DbSessionFuture<ResultSet> future : futures) {
			assertTrue(future.isDone(), "Future did not finish before connection was closed.");
			assertTrue(future.isCancelled(), "Future should have been cancelled at close");
		}
		logger.debug("Closing locking connection");
		lockingConnection.rollback().get();
		lockingConnection.close(true).get();
		logger.debug("Locking connection close");
	}
}
