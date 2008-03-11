package org.adbcj.tck.test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSessionFuture;
import org.adbcj.ResultSet;
import org.adbcj.tck.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 *
 */
public class ConnectSpecialCaseTest {
	//private static final String UNREACHABLE_HOST = "1.0.0.1";

	private final Logger logger = LoggerFactory.getLogger(ConnectSpecialCaseTest.class);
	
	@Parameters({"url", "user", "password"})
	@Test(timeOut=60000)
	public void testConnectBadCredentials(String url, String user, String password) throws InterruptedException {
		final boolean[] callbacks = {false};
		final CountDownLatch latch = new CountDownLatch(1);

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, "__BADPASSWORD__");
		try {
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
			connectionManager.close(true);
		}
	}

//	@Parameters({"url", "user", "password"})
//	@Test(timeOut=60000)
//	public void testConnectCancel(String url, String user, String password) throws Exception {
//		StringBuilder urlBuilder = new StringBuilder();
//
//		URI connectUrl = new URI(url);
//		String scheme = connectUrl.getScheme();
//		while (scheme != null) {
//			urlBuilder.append(scheme).append(":");
//			connectUrl = new URI(connectUrl.getSchemeSpecificPart());
//			scheme = connectUrl.getScheme();
//		}
//
//		urlBuilder.append("//").append(UNREACHABLE_HOST);
//		urlBuilder.append(connectUrl.getPath());
//
//		final boolean[] callbacks = {false};
//		final CountDownLatch latch = new CountDownLatch(1);
//
//		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(urlBuilder.toString(), "dummyuser", "dummypassword");
//		try {
//			DbFuture<Connection> connectFuture = connectionManager.connect().addListener(new DbListener<Connection>() {
//				public void onCompletion(DbFuture<Connection> future) throws Exception {
//					callbacks[0] = true;
//					latch.countDown();
//				}
//			});
//			assertTrue(connectFuture.cancel(true), "Connection to unreachable host was not canceled");
//			assertTrue(connectFuture.isCancelled());
//			assertTrue(latch.await(1, TimeUnit.SECONDS), "Callback was not invoked in time");
//			assertTrue(callbacks[0], "Connect future callback was not invoked with connect cancellation");
//		} finally {
//			connectionManager.close(true);
//		}
//	}

	@Parameters({"url", "user", "password"})
	@Test(timeOut=60000)
	public void testImmediateClose(String url, String user, String password) throws InterruptedException {
		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, password);
		try {
			Connection lockingConnection = connectionManager.connect().get();
			Connection connection = connectionManager.connect().get();
			connection.setPipeliningEnabled(false);
	
			lockingConnection.beginTransaction();
			TestUtils.selectForUpdate(lockingConnection).get();
	
			List<DbSessionFuture<ResultSet>> futures = new ArrayList<DbSessionFuture<ResultSet>>();
	
			connection.beginTransaction();
	
			TestUtils.selectForUpdate(connection);
			for (int i = 0; i < 5; i++) {
				futures.add(connection.executeQuery(String.format("SELECT *, %d FROM simple_values", i)));
			}
	
			logger.debug("Closing connection");
			connection.close(true).get();
			logger.debug("Closed");
	
			logger.debug("Closing locking connection");
			lockingConnection.rollback().get();
			lockingConnection.close(true).get();
			logger.debug("Locking connection close");
	
			assertTrue(connection.isClosed(), "Connection should be closed");
			for (DbSessionFuture<ResultSet> future : futures) {
				assertTrue(future.isCancelled(), "Future should have been cancelled at close: " + future);
				assertTrue(future.isDone(), "Request did not finish before connection was closed: " + future);
			}
		} finally {
			connectionManager.close(true).get();
		}
	}

}
