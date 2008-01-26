package org.adbcj.tck;

import java.util.concurrent.atomic.AtomicBoolean;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SelectForUpdateTest extends ConnectionManagerDataProvider {

	private final Logger logger = LoggerFactory.getLogger(SelectForUpdateTest.class);
	
	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testSelectForUpdate(ConnectionManager connectionManager) throws Exception {
		logger.debug("Using connection manager: {}", connectionManager);
		final boolean[] invoked = {false, false};
		final AtomicBoolean locked = new AtomicBoolean(false);
		final AtomicBoolean error = new AtomicBoolean(false);
		
		Connection conn1 = connectionManager.connect().get();
		Connection conn2 = connectionManager.connect().get();
		logger.debug("Obtained connection managers");
		
		// Get lock on locks table
		conn1.beginTransaction();
		TestUtils.selectForUpdate(conn1, new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> future) throws Exception {
				logger.debug("In first callback");
				locked.set(true);
				invoked[0] = true;
			}
		}).get();
		logger.debug("Obtained lock on locks table");
		
		// Try to get lock with second connection
		conn2.beginTransaction();
		DbFuture<ResultSet> future = TestUtils.selectForUpdate(conn2, new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> future) throws Exception {
				logger.debug("In second callback");
				invoked[1] = true;
				if (!locked.get()) {
					error.set(true);
				}
			}
		});
		logger.debug("Select for update called with second connection, should be blocking");
		
		assertTrue(invoked[0], "First SELECT FOR UPDATE callback should have been invoked");
		assertTrue(locked.get(), "locked should be set");
		assertFalse(invoked[1], "Second SELCT FOR UPDATE callback should not have been invoked yet");
		assertFalse(error.get());
		
		conn1.rollback().get();
		logger.debug("Released first lock");
		
		future.get();
		logger.debug("Second SELECT FOR UPDATE completed");
		
		assertTrue(invoked[1]);
		assertFalse(error.get(), "An error occurred during SELECT FOR UPDATE");
		conn2.rollback().get();
		logger.debug("Released second lock");
		
		// Close connections
		logger.debug("Closing connections");
		conn1.close(true).get();
		logger.debug("Closed connection 1");
		conn2.close(true).get();
		logger.debug("Closed connection 2");
	}
	
}
