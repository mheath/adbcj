package org.safehaus.adbcj.tck;

import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbListener;
import org.safehaus.adbcj.ResultSet;

public class TestUtils {

	public static final String DEFAULT_LOCK_NAME = "lock";
	
	public static DbFuture<ResultSet> selectForUpdate(Connection connection) throws InterruptedException {
		return selectForUpdate(connection, DEFAULT_LOCK_NAME, null);
	}
	
	public static DbFuture<ResultSet> selectForUpdate(Connection connection, DbListener<ResultSet> listener) throws InterruptedException {
		return selectForUpdate(connection, DEFAULT_LOCK_NAME, listener);
	}
	
	public static DbFuture<ResultSet> selectForUpdate(Connection connection, String lock, DbListener<ResultSet> listener) throws InterruptedException {
		if (!connection.isInTransaction()) {
			throw new IllegalStateException("You must be in a transaction for a select for update to work");
		}
		DbFuture<ResultSet> future = connection.executeQuery("SELECT name FROM locks WHERE name='lock' FOR UPDATE");
		if (listener != null) {
			future.addListener(listener);
		}
		return future;
	}
	
}
