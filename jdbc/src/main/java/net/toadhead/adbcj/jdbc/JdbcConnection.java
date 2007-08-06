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
package net.toadhead.adbcj.jdbc;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;
import edu.byu.cs.adbcj.support.BaseRequestQueue;
import edu.byu.cs.adbcj.support.ConcurrentFutureSessionProxy;
import edu.byu.cs.adbcj.support.DbSessionFutureProxy;
import edu.byu.cs.adbcj.support.RequestAction;

public class JdbcConnection extends BaseRequestQueue implements Connection {

	private final JdbcConnectionManager connectionManager;
	private final java.sql.Connection jdbcConnection;
	
	private DbFuture<Void> closeFuture;
	private volatile boolean closed = false;
	
	public JdbcConnection(JdbcConnectionManager connectionManager, java.sql.Connection jdbcConnection) {
		this.connectionManager = connectionManager;
		this.jdbcConnection = jdbcConnection;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public void beginTransaction() {
		// TODO Auto-generated method stub
		
	}

	public DbSessionFuture<Void> commit() {
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<Void> rollback() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isInTransaction() {
		// TODO Auto-generated method stub
		return false;
	}

	public TransactionIsolationLevel getTransactionIsolationLevel() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTransactionIsolationLevel(
			TransactionIsolationLevel transactionIsolationLevel) {
		// TODO Auto-generated method stub
		
	}

	public synchronized DbSessionFuture<Void> close(boolean immediate) throws DbException {
		if (!closed) {
			closed = true;
			
			if (immediate) {
				cancelPendingRequests(true);
				final ConcurrentFutureSessionProxy<Void> localFuture = new ConcurrentFutureSessionProxy<Void>(this) {
					/**
					 * If the future gets cancelled, unclose the connection.
					 */
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						boolean cancelled = super.cancel(mayInterruptIfRunning);
						if (cancelled) {
							unclose();
						}
						return cancelled;
					}
				}; 
				Future<Void> future = connectionManager.getExecutorService().submit(new Callable<Void>() {
					public Void call() throws Exception {
						try {
							jdbcConnection.close();
						} finally {
							localFuture.setDone();
						}
						return null;
					}
				});
				localFuture.setFuture(future);
				closeFuture = localFuture;
			} else {
				AbstractDbFutureBase<Void> localFuture = enqueueRequest(new RequestAction<Void>() {
					Future<Void> future = null;
					public synchronized boolean cancel(boolean mayInterruptIfRunning) {
						if (future == null) {
							return false;
						}
						return future.cancel(mayInterruptIfRunning);
					}
					public synchronized void execute(final AbstractDbFutureBase<Void> future) {
						this.future = connectionManager.getExecutorService().submit(new Callable<Void>() {
							public Void call() throws Exception {
								try {
									jdbcConnection.close();
								} finally {
									future.setDone();
								}
								return null;
							}
						});
					}
				});
				closeFuture = localFuture;
			}
		}
		return newFutureProxy(closeFuture);
	}

	private synchronized void unclose() {
		this.closeFuture = null;
		closed = false;
	}
	
	public boolean isClosed() {
		try {
			return closed && jdbcConnection.isClosed();
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public DbSessionFuture<ResultSet> executeQuery(String sql) {
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		// TODO Auto-generated method stub
		return null;
	}

	public DbFuture<Void> ping() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * 
	 * End of API methods 
	 * 
	 */

	@SuppressWarnings("unchecked")
	private <E> DbSessionFuture<E> newFutureProxy(DbFuture<?> future) {
		return new DbSessionFutureProxy<E>((DbFuture<E>)future, this);
	}

}
