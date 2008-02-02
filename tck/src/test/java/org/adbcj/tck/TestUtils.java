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
package org.adbcj.tck;

import org.adbcj.Connection;
import org.adbcj.DbListener;
import org.adbcj.DbSessionFuture;
import org.adbcj.ResultSet;

public class TestUtils {

	public static final String DEFAULT_LOCK_NAME = "lock";
	
	public static DbSessionFuture<ResultSet> selectForUpdate(Connection connection) throws InterruptedException {
		return selectForUpdate(connection, DEFAULT_LOCK_NAME, null);
	}
	
	public static DbSessionFuture<ResultSet> selectForUpdate(Connection connection, DbListener<ResultSet> listener) throws InterruptedException {
		return selectForUpdate(connection, DEFAULT_LOCK_NAME, listener);
	}
	
	public static DbSessionFuture<ResultSet> selectForUpdate(Connection connection, String lock, DbListener<ResultSet> listener) throws InterruptedException {
		if (!connection.isInTransaction()) {
			throw new IllegalStateException("You must be in a transaction for a select for update to work");
		}
		DbSessionFuture<ResultSet> future = connection.executeQuery("SELECT name FROM locks WHERE name='lock' FOR UPDATE");
		if (listener != null) {
			future.addListener(listener);
		}
		return future;
	}
	
}
