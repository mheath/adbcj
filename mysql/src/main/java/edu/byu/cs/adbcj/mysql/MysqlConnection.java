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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.support.AbstractTransactionalSession;
import edu.byu.cs.adbcj.support.DefaultDbFuture;
import edu.byu.cs.adbcj.support.DefaultDbSessionFuture;
import edu.byu.cs.adbcj.support.Request;

public class MysqlConnection extends AbstractTransactionalSession implements Connection {
	private final Logger logger = LoggerFactory.getLogger(MysqlConnection.class);

	private final ConnectionManager connectionManager;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;

	private DefaultDbSessionFuture<Void> closeFuture;
	private volatile boolean closed = false;
	
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
				closeFuture = enqueueRequest(new Request<Void>() {
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

	public DbSessionFuture<ResultSet> executeQuery(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<ResultSet>() {
			public void execute(DefaultDbFuture<ResultSet> future) {
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
	}
	
	public DbSessionFuture<Result> executeUpdate(final String sql) {
		checkClosed();
		logger.info("Scheduling update '{}'", sql);
		return enqueueTransactionalRequest(new Request<Result>() {
			public void execute(DefaultDbFuture<Result> future) {
				logger.info("Sending update '{}'", sql);
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
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

	// ************* Transaction method implementations ******************************************
	private void sendCommit() {
		CommandRequest request = new CommandRequest(Command.QUERY, "commit");
		session.write(request);
	}
	
	private void sendRollback() {
		CommandRequest request = new CommandRequest(Command.QUERY, "rollback");
		session.write(request);
	}
	
	@Override
	protected DbSessionFuture<Void> enqueueStartTransaction(final Transaction transaction) {
		Request<Void> request = new Request<Void>() {
			@Override
			public void execute(DefaultDbFuture<Void> future) {
				synchronized (MysqlConnection.this) {
					transaction.setStarted(false);
					CommandRequest request = new CommandRequest(Command.QUERY, "begin");
					session.write(request);
				}
			}
		};
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}
	
	@Override
	protected DbSessionFuture<Void> enqueueCommit(final Transaction transaction) {
		return enqueueRequest(new Request<Void>() {
			private volatile boolean executing = false;
			private volatile boolean cancelled = false;
			public synchronized void execute(DefaultDbFuture<Void> future) {
				executing = true;
				if (cancelled) {
					if (transaction.isStarted()) {
						sendRollback();
					}
				} else {
					sendCommit();
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
				transaction.cancelPendingRequests();
				return true;
			}
			// Commit can not be removed - if commit gets cancelled, it is converted to a rollback
			@Override
			public boolean canRemove() {
				return false;
			}
		});
	}
	
	@Override
	protected DbSessionFuture<Void> enqueueRollback(Transaction transaction) {
		return enqueueRequest(new Request<Void>() {
			@Override
			public void execute(DefaultDbFuture<Void> future) {
				sendRollback();
			}
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}
			@Override
			public boolean canRemove() {
				return false;
			}
		});
	}
	
	@Override
	protected DbSessionFuture<Void> enqueueChangeIsolationLevel(Transaction transaction,
			final TransactionIsolationLevel transactionIsolationLevel) {
		Request<Void> request = new Request<Void>() {
			@Override
			public void execute(DefaultDbFuture<Void> future) {
				// TODO Set transaction isolation level
			}
		};
		transaction.addRequest(request);
		return enqueueRequest(request);
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

	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	protected void checkClosed() {
		if (closed) {
			throw new DbException("This connection has been closed");
		}
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
	public <E> DefaultDbSessionFuture<E> enqueueRequest(Request<E> request) {
		return super.enqueueRequest(request);
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
	public <E> Request<E> makeNextRequestActive() {
		return super.makeNextRequestActive();
	}

}
