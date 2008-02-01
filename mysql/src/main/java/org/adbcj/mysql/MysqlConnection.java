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
package org.adbcj.mysql;

import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.common.IoSession;
import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.ResultEventHandler;
import org.adbcj.DbSessionFuture;
import org.adbcj.PreparedStatement;
import org.adbcj.Result;
import org.adbcj.support.AbstractTransactionalSession;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;
import org.adbcj.support.Request;
import org.adbcj.mysql.MysqlConnectionManager.MysqlConnectFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlConnection extends AbstractTransactionalSession implements Connection {
	private final Logger logger = LoggerFactory.getLogger(MysqlConnection.class);

	private final int id;
	
	private final ConnectionManager connectionManager;
	private final MysqlConnectFuture connectFuture;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;

	private DefaultDbSessionFuture<Void> closeFuture;
	
	public MysqlConnection(ConnectionManager connectionManager, MysqlConnectFuture connectFuture, IoSession session, LoginCredentials credentials, int id) {
		this.connectionManager = connectionManager;
		this.connectFuture = connectFuture;
		this.session = session;
		this.credentials = credentials;
		this.id = id;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public synchronized DbSessionFuture<Void> close(final boolean immediate) throws DbException {
		// If the connection is already closed, return existing close future
		logger.debug("Closing");
		if (isClosed()) {
			if (closeFuture == null) {
				closeFuture = new DefaultDbSessionFuture<Void>(this);
				closeFuture.setResult(null);
			}
			return closeFuture;
		} else {
			if (immediate) {
				logger.debug("Executing immediate close");
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
					private boolean cancelled = false;
					@Override
					public synchronized boolean cancel(boolean mayInterruptIfRunning) {
						if (!requestClosed) {
							logger.debug("Cancelling close");
							cancelled = true;
							unclose();
							return true;
						}
						logger.debug("Close in progress, cannot cancel");
						return false;
					}
					public synchronized void execute() {
						if (cancelled) {
							makeNextRequestActive();
							return;
						}
						logger.debug("Executing deferred close");
						requestClosed = true;

						logger.debug("Writing QUIT");
						session.write(new CommandRequest(Command.QUIT));
					}
				});
			}
		}
		logger.trace("Exiting close()");
		return closeFuture;
	}
	
	private synchronized void unclose() {
		logger.debug("Unclosing");
		this.closeFuture = null;
	}
	
	public synchronized boolean isClosed() {
		return closeFuture != null || session.isClosing();
	}

	public <T> DbSessionFuture<T> executeQuery(final String sql, ResultEventHandler<T> eventHandler, T accumulator) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<T>(eventHandler, accumulator) {
			@Override
			public void execute() throws Exception {
				logger.debug("Sending query '{}'", sql);
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
	}
	
	public DbSessionFuture<Result> executeUpdate(final String sql) {
		checkClosed();
		logger.debug("Scheduling update '{}'", sql);
		return enqueueTransactionalRequest(new Request<Result>() {
			public void execute() {
				logger.debug("Sending update '{}'", sql);
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		checkClosed();
		// TODO Implement MySQL prepareStatement(String sql)
		throw new IllegalStateException("Not yet implemented");
	}
	
	public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
		checkClosed();
		// TODO Implement MySQL prepareStatement(Object key, String sql)
		throw new IllegalStateException("Not yet implemented");
	}
	
	public DbFuture<Void> ping() {
		checkClosed();
		// TODO Implement MySQL ping()
		throw new IllegalStateException("Not yet implemented");
	}

	// ************* Transaction method implementations ******************************************
	
	private static final CommandRequest BEGIN = new CommandRequest(Command.QUERY, "begin");
	private static final CommandRequest COMMIT = new CommandRequest(Command.QUERY, "commit"); 
	private static final CommandRequest ROLLBACK = new CommandRequest(Command.QUERY, "rollback"); 
	
	@Override
	protected void sendCommit() {
		session.write(COMMIT);
	}
	
	@Override
	protected void sendRollback() {
		session.write(ROLLBACK);
	}
	
	@Override
	protected void sendBegin() {
		session.write(BEGIN);
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

	protected void checkClosed() {
		if (isClosed()) {
			throw new DbException(this, "This connection has been closed");
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
	 * Make this method public
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
	
	public MysqlConnectFuture getConnectFuture() {
		return connectFuture;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MysqlConnection)) {
			return false;
		}
		return id == ((MysqlConnection)obj).id;
	}

}
