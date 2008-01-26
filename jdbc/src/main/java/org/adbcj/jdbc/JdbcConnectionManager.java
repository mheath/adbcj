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
package org.adbcj.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbSessionFuture;

import org.adbcj.support.DbFutureConcurrentProxy;

public class JdbcConnectionManager implements ConnectionManager {

	private static final Object USER = "user";
	private static final Object PASSWORD = "password";
	
	private final String jdbcUrl;
	private final Properties properties;
	private final ExecutorService executorService;

	public JdbcConnectionManager(String jdbcUrl, String username,
			String password, Properties properties) {
		this.jdbcUrl = jdbcUrl;
		this.properties = new Properties(properties);
		// TODO Make it so we can configure the parameters to the thread pool
		this.executorService = Executors.newCachedThreadPool();

		this.properties.put(USER, username);
		this.properties.put(PASSWORD, password);
	}

	public DbFuture<Connection> connect() throws DbException {
		final DbFutureConcurrentProxy<Connection> future = new DbFutureConcurrentProxy<Connection>();
		Future<Connection> executorFuture = executorService.submit(new Callable<Connection>() {
			public Connection call() throws Exception {
				try {
					java.sql.Connection jdbcConnection = DriverManager.getConnection(jdbcUrl, properties);
					JdbcConnection connection = new JdbcConnection(JdbcConnectionManager.this, jdbcConnection);
					future.setValue(connection);
					return connection;
				} catch (SQLException e) {
					future.setException(new DbException(e));
					throw e;
				} finally {
					future.setDone();
				}
			}
		});
		future.setFuture(executorFuture);
		return future;
	}

	public DbSessionFuture<Void> close(boolean immediate) throws DbException {
		// TODO Implement JdbcConnectionManager.close(boolean immediate)
		throw new IllegalStateException("Not implemented");
	}

	public boolean isClosed() {
		// TODO Implement JdbcConnectionManager.isClosed()
		throw new IllegalStateException("Not implemented");
	}

	/*
	 * 
	 * Non API Method
	 * 
	 */
	
	public ExecutorService getExecutorService() {
		return executorService;
	}

	@Override
	public String toString() {
		return String.format("%s: %s (user: %s)", getClass().getName(), jdbcUrl, properties.get(USER));
	}
	
}
