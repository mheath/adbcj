/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.tck.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.adbcj.Value;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(timeOut = 5000)
public class TransactionTest {

	private ConnectionManager connectionManager;

	@Parameters({"url", "user", "password"})
	@BeforeTest
	public void createConnectionManager(String url, String user, String password) {
		connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, password);
	}

	@AfterTest
	public void closeConnectionManager() {
		DbFuture<Void> closeFuture = connectionManager.close(true);
		closeFuture.getUninterruptably();
	}

	public void testBeginTransaction() throws Exception {
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
	
	public void testCommitRollbackWithNoTransaction() throws Exception {
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
	
	public void testRollback() throws Exception {
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

	public void testCommit() throws Exception {
		Connection connection = connectionManager.connect().get();
		Connection connection2 = connectionManager.connect().get();
		try {
			// Clear out updates table
			Result result = connection.executeUpdate("DELETE FROM updates").get();
			assertNotNull(result);

			connection.beginTransaction();

			// Insert a row
			result = connection.executeUpdate("INSERT INTO updates (id) VALUES (1)").get();
			assertNotNull(result);
			assertEquals(result.getAffectedRows(), Long.valueOf(1));
			
			// Make sure second connection can't see data
			ResultSet rs = connection2.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 0);
			
			connection.commit().get();
			
			// Make sure both connections can see data
			rs = connection.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 1);
			assertEquals(rs.get(0).get(0).getInt(), 1);
			
			rs = connection2.executeQuery("SELECT id FROM updates").get();
			assertNotNull(rs);
			assertEquals(rs.size(), 1);
			assertEquals(rs.get(0).get(0).getInt(), 1);
			
		} finally {
			connection.close(true);
			connection2.close(true);
		}
	}
}
