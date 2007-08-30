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
package edu.byu.cs.adbcj.mysql;

import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.common.IoSession;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.support.DefaultDbFuture;
import edu.byu.cs.adbcj.support.DefaultDbSessionFuture;
import edu.byu.cs.adbcj.support.AbstractSessionRequestQueue;
import edu.byu.cs.adbcj.support.RequestAction;
import edu.byu.cs.adbcj.support.TransactionHelper;

public class MysqlConnection extends AbstractSessionRequestQueue implements Connection {

	private final ConnectionManager connectionManager;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;

	private DefaultDbSessionFuture<Void> closeFuture;
	private volatile boolean closed = false;
	
	private TransactionHelper transactionHelper;
	private volatile TransactionIsolationLevel transactionIsolationLevel = TransactionIsolationLevel.READ_COMMITTED;
	private volatile TransactionIsolationLevel actualTransactionIsolationlevel = TransactionIsolationLevel.READ_COMMITTED;
	
	public MysqlConnection(ConnectionManager connectionManager, IoSession session, LoginCredentials credentials) {
		this.connectionManager = connectionManager;
		this.session = session;
		this.credentials = credentials;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public synchronized DbSessionFuture<Void> close(final boolean immediate) throws DbException {
		// If the connection is already closed, return existing close future
		if (!closed) {
			closed = true;
	
			if (immediate) {
				// If the close is immediate, cancel pending requests and send request to server
				cancelPendingRequests(true);
				session.write(new CommandRequest(Command.QUIT));
				closeFuture = new DefaultDbSessionFuture<Void>(this) {
					@Override
					protected boolean doCancel(boolean mayInterruptIfRunning) {
						// Canceling is not possible when an immediate close
						return false;
					}
				};
			} else {
				// If the close is NOT immediate, schedule the close
				closeFuture = enqueueRequest(new RequestAction<Void>() {
					private boolean requestClosed = false;
					@Override
					public synchronized boolean cancel(boolean mayInterruptIfRunning) {
						if (!requestClosed) {
							unclose();
							return true;
						}
						return false;
					}
					public synchronized void execute(DefaultDbFuture<Void> future) {
						requestClosed = true;
						// Do a close immediate to close the connection
						close(true);
					}
				});
			}
		}
		
		return closeFuture;
	}
	
	private synchronized void unclose() {
		this.closeFuture = null;
		closed = false;
	}
	
	public boolean isClosed() {
		return closed;
	}

	public TransactionIsolationLevel getTransactionIsolationLevel() {
		checkClosed();
		return transactionIsolationLevel;
	}

	public synchronized void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel) {
		checkClosed();
		if (transactionIsolationLevel == null) {
			throw new NullPointerException("Cannot set transactionIsolationLevel to null");
		}
		if (transactionHelper == null || !transactionHelper.isStarted()) {
			this.transactionIsolationLevel = transactionIsolationLevel; 
		} else {
			throw new DbException("Can't change transaction isolation level of running transaction.");
		}
	}

	public synchronized void beginTransaction() {
		checkClosed();
		if (isInTransaction()) {
			throw new DbException("Cannot begin new transaction.  Current transaction needs to be committed or rolled back");
		}
		transactionHelper = new TransactionHelper();
	}

	public synchronized boolean isInTransaction() {
		checkClosed();
		return transactionHelper != null;
	}

	public synchronized DbSessionFuture<Void> commit() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException("Not currently in a transaction, cannot commit");
		}
		return enqueueRequest(new RequestAction<Void>() {
			private TransactionHelper transactionHelper = MysqlConnection.this.transactionHelper;
			private boolean executing = false;
			private boolean cancelled = false;
			public synchronized void execute(DefaultDbFuture<Void> future) {
				executing = true;
				if (cancelled) {
					// TODO: If no requests from transaction have been sent, then we don't have to send rollback because transaction was never started 
					// TODO: Send rollback
				} else {
					// TODO: Send commit
				}
			}
			@Override
			public synchronized boolean cancel(boolean mayInterruptIfRunning) {
				// If commit has already started, it can't be stopped
				if (executing) {
					return false;
				}
				// If commit is not executing, indicate that commit has been canceled and do rollback
				cancelled = true;
				cancelPendingRequests(transactionHelper);
				return true;
			}
			// Commit can not be removed - if commit gets cancelled, it is converted to a rollback
			@Override
			public boolean canRemove() {
				return false;
			}
		});
	}

	public synchronized DbSessionFuture<Void> rollback() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException("Not currently in a transaction, cannot rollback");
		}
		cancelPendingRequests(transactionHelper);
		if (transactionHelper.isStarted()) {
			return enqueueRequest(new RequestAction<Void>() {
				public void execute(DefaultDbFuture<Void> future) {
					// TODO: Schedule rollback
				}
				// Rollbacks can not be canceled
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					return false;
				}
				// Rollbacks can not be canceled
				@Override
				public boolean canRemove() {
					return false;
				}
			});
		}
		return new DefaultDbSessionFuture<Void>(this) {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				return false;
			}
		};
	}

	private synchronized void cancelPendingRequests(TransactionHelper transactionHelper) {
		for (Request<?> request : transactionHelper.getRequests()) {
			if (removeRequest(request)) {
				request.getFuture().cancel(false);
			}
		}
	}
	
	public DbSessionFuture<ResultSet> executeQuery(final String sql) {
		checkClosed();
		// TODO Start transaction if transaction needs to be started
		DbSessionFuture<ResultSet> future = enqueueRequest(new RequestAction<ResultSet>() {
			public void execute(DefaultDbFuture<ResultSet> future) {
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
		return future;
	}
	
	public DbSessionFuture<Result> executeUpdate(String sql) {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}
	
	public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public DbFuture<Void> ping() {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	// ************* Non-API methods *************************************************************
	
	public ServerGreeting getServerGreeting() {
		return serverGreeting;
	}

	public void setServerGreeting(ServerGreeting serverGreeting) {
		this.serverGreeting = serverGreeting;
	}

	public synchronized DefaultDbFuture<Void> getCloseFuture() {
		return closeFuture;
	}
	
	public LoginCredentials getCredentials() {
		return credentials;
	}

	public IoSession getIoSession() {
		return session;
	}

	public MysqlCharacterSet getCharacterSet() {
		if (serverGreeting == null) {
			return MysqlCharacterSet.LATIN1_SWEDISH_CI;
		} else {
			return serverGreeting.getCharacterSet();
		}
	}
	
	private void checkClosed() {
		if (closed) {
			throw new DbException("This connection has been closed");
		}
	}

	private static final Set<ClientCapabilities> CLIENT_CAPABILITIES = EnumSet.of(
			ClientCapabilities.LONG_PASSWORD,
			ClientCapabilities.FOUND_ROWS,
			ClientCapabilities.LONG_COLUMN_FLAG,
			ClientCapabilities.CONNECT_WITH_DB,
			ClientCapabilities.LOCAL_FILES,
			ClientCapabilities.PROTOCOL_4_1,
			ClientCapabilities.TRANSACTIONS,
			ClientCapabilities.SECURE_AUTHENTICATION);

	public Set<ClientCapabilities> getClientCapabilities() {
		return CLIENT_CAPABILITIES;
	}

	private static final Set<ExtendedClientCapabilities> EXTENDED_CLIENT_CAPABILITIES = EnumSet.of(
			ExtendedClientCapabilities.MULTI_RESULTS
			);

	public Set<ExtendedClientCapabilities> getExtendedClientCapabilities() {
		return EXTENDED_CLIENT_CAPABILITIES;
	}

	//
	//
	// Queuing methods
	//
	//
	
	/*
	 * Make this method public.
	 */
	@Override
	public synchronized <E> DefaultDbSessionFuture<E> enqueueRequest(RequestAction<E> action) {
		return super.enqueueRequest(action);
	}
	
	/*
	 * Make this method public.
	 */
	@Override
	public <E> Request<E> getActiveRequest() {
		return super.getActiveRequest();
	}
	
	/*
	 * Make this method public.
	 */
	@Override
	public synchronized <E> Request<E> makeNextRequestActive() {
		return super.makeNextRequestActive();
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}
}
