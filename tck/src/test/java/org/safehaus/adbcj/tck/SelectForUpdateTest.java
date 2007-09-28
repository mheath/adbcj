package org.safehaus.adbcj.tck;

import java.util.concurrent.atomic.AtomicBoolean;

import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbListener;
import org.safehaus.adbcj.ResultSet;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SelectForUpdateTest extends ConnectionManagerDataProvider {

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testSelectForUpdate(ConnectionManager connectionManager) throws Exception {
		final boolean[] invoked = {false, false};
		final AtomicBoolean locked = new AtomicBoolean(false);
		final AtomicBoolean error = new AtomicBoolean(false);
		
		Connection conn1 = connectionManager.connect().get();
		Connection conn2 = connectionManager.connect().get();
		
		// Get lock on locks table
		conn1.beginTransaction();
		TestUtils.selectForUpdate(conn1, new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> future) throws Exception {
				locked.set(true);
				invoked[0] = true;
			}
		}).get();
		System.out.println("Obtained lock on locks table");
		
		// Try to get lock with second connection
		conn2.beginTransaction();
		DbFuture<ResultSet> future = TestUtils.selectForUpdate(conn2, new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> future) throws Exception {
				System.out.println("In second callback");
				invoked[1] = true;
				if (!locked.get()) {
					error.set(true);
				}
			}
		});
		
		assertTrue(invoked[0], "First SELECT FOR UPDATE callback should have been invoked");
		assertTrue(locked.get(), "locked should be set");
		assertFalse(invoked[1], "Second SELCT FOR UPDATE callback should not have been invoked yet");
		assertFalse(error.get());
		
		conn1.rollback().get();
		System.out.println("Released first lock");
		
		future.get();
		assertTrue(invoked[1]);
		assertFalse(error.get(), "An error occurred during SELECT FOR UPDATE");
		conn2.rollback().get();
		System.out.println("Released second lock");
		
		// Close connections
		System.out.println("Closing");
		conn1.close(true).get();
		System.out.println("Closed 1");
		conn2.close(true).get();
		System.out.println("Closed 2");
	}
	
}
