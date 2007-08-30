package org.safehaus.adbcj.tck;

import org.testng.Assert;
import org.testng.annotations.Test;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;

public class TransactionTest extends ConnectionManagerDataProvider {

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testBeginTransaction(ConnectionManager connectionManager) throws Exception {
		Connection connection = connectionManager.connect().get();
		Assert.assertTrue(!connection.isInTransaction(), "Connections should not start with transaction started");
		connection.beginTransaction();
		Assert.assertTrue(connection.isInTransaction(), "Connection should be in transaction");
		try {
			connection.beginTransaction();
			Assert.fail("Should have thrown exception because connection is already in transaction");
		} catch (DbException e) {
			// Pass
		}
	}
	
}
