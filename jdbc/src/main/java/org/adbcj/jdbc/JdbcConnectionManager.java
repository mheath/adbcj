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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.support.DbFutureConcurrentProxy;
import org.adbcj.support.DefaultDbFuture;

public class JdbcConnectionManager implements ConnectionManager {

	private static final Object USER = "user";
	private static final Object PASSWORD = "password";
	
	private final String jdbcUrl;
	private final Properties properties;
	private final ExecutorService executorService;

	private final Object lock = this;
	private final Set<JdbcConnection> connections = new HashSet<JdbcConnection>(); // Access must be synchronized on lock 
	
	private volatile DefaultDbFuture<Void> closeFuture;
	
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
		if (isClosed()) {
			throw new DbException("This connection manager is closed");
		}
		final DbFutureConcurrentProxy<Connection> future = new DbFutureConcurrentProxy<Connection>();
		Future<Connection> executorFuture = executorService.submit(new Callable<Connection>() {
			public Connection call() throws Exception {
				try {
					java.sql.Connection jdbcConnection = DriverManager.getConnection(jdbcUrl, properties);
					JdbcConnection connection = new JdbcConnection(JdbcConnectionManager.this, jdbcConnection);
					synchronized (lock) {
						if (isClosed()) {
							connection.close(true);
							future.setException(new DbException("Connection manager closed"));
						} else {
							connections.add(connection);
							future.setValue(connection);
						}
					}
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

	public DbFuture<Void> close(boolean immediate) throws DbException {
		synchronized (lock) {
			if (closeFuture == null) {
				closeFuture = new DefaultDbFuture<Void>();
				closeFuture.addListener(new DbListener<Void>() {
					@Override
					public void onCompletion(DbFuture<Void> future) throws Exception {
						executorService.shutdown();
					}
				});
			} else {
				return closeFuture;
			}
		}
		final AtomicInteger countDown = new AtomicInteger();
		final AtomicBoolean allClosed = new AtomicBoolean(false);
		
		DbListener<Void> listener = new DbListener<Void>() {
			@Override
			public void onCompletion(DbFuture<Void> future) {
				try {
					int count = countDown.decrementAndGet();
					future.get();
					if (allClosed.get() && count == 0) {
						closeFuture.setResult(null);
					}
				} catch (Exception e) {
					// If the connection close errored out, error out our closeFuture too
					closeFuture.setException(e);
				}
			}
		};
		synchronized (lock) {
			for (JdbcConnection connection : connections) {
				countDown.incrementAndGet();
				connection.close(immediate).addListener(listener);
			}
		}
		allClosed.set(true);
		if (countDown.get() == 0) {
			closeFuture.setResult(null);
		}
		return closeFuture;
	}

	public boolean isClosed() {
		return closeFuture != null;
	}

	/*
	 * 
	 * Non API Method
	 * 
	 */
	
	public ExecutorService getExecutorService() {
		return executorService;
	}

	boolean removeConnection(JdbcConnection connection) {
		synchronized (lock) {
			return connections.remove(connection);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s (user: %s)", getClass().getName(), jdbcUrl, properties.get(USER));
	}
	
}
