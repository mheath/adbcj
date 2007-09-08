package org.safehaus.adbcj.tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.Value;

public class TransactionTest extends ConnectionManagerDataProvider {

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testBeginTransaction(ConnectionManager connectionManager) throws Exception {
		Connection connection = connectionManager.connect().get();
		try {
			Assert.assertTrue(!connection.isInTransaction(), "Connections should not start with transaction started");
			connection.beginTransaction();
			Assert.assertTrue(connection.isInTransaction(), "Connection should be in transaction");
			try {
				connection.beginTransaction();
				Assert.fail("Should have thrown exception because connection is already in transaction");
			} catch (DbException e) {
				// Pass
			}
		} finally {
			connection.close(true);
		}
	}
	
	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testCommitRollbackWithNoTransaction(ConnectionManager connectionManager) throws Exception {
		Connection connection = connectionManager.connect().get();
		try {
			// Test commit with no transaction
			try {
				connection.commit();
				Assert.fail("Not in transaction, commit should have failed");
			} catch (DbException e) {
				// Pass
			}

			// Test rollback with no transaction
			try {
				connection.rollback();
				Assert.fail("Not in transaction, rollback should have failed");
			} catch (DbException e) {
				// Pass
			}
			
			connection.beginTransaction();
			connection.rollback().get();
			
			connection.beginTransaction();
			connection.commit().get();
			
			connection.beginTransaction();
		} finally {
			connection.close(true);
		}
	}
	
	@Test(dataProvider="connectionManagerDataProvider")
	public void testRollback(ConnectionManager connectionManager) throws Exception {
		Connection connection = connectionManager.connect().get();
		try {
			// Clear out updates table
			Result result = connection.executeUpdate("DELETE FROM updates").get();
			assertNotNull(result);

			connection.beginTransaction();
			
			// Insert a row
			result = connection.executeUpdate("INSERT INTO updates (id) VALUES (1)").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(1));

			// Make sure we can select the row
			ResultSet rs = connection.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 1);
			Value value = rs.get(0).get(0);
			assertEquals(value.getInt(), 1);

			// Rollback transaction
			connection.rollback().get();
			
			// select query should now be empty 
			rs = connection.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 0);

		} finally {
			connection.close(true);
		}
	}
	
}
