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

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbSessionClosedException;
import org.adbcj.DbSessionFuture;
import org.adbcj.PreparedStatement;
import org.adbcj.Result;
import org.adbcj.ResultEventHandler;
import org.adbcj.mysql.MysqlConnectionManager.MysqlConnectFuture;
import org.adbcj.support.AbstractDbSession;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlConnection extends AbstractDbSession implements Connection {
	private final Logger logger = LoggerFactory.getLogger(MysqlConnection.class);

	private final int id;
	
	private final ConnectionManager connectionManager;
	private final MysqlConnectFuture connectFuture;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;

	private Request<Void> closeRequest;
	
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
			if (closeRequest == null) {
				closeRequest = new Request<Void>() {
					@Override
					public void execute() throws Exception {
						// Do nothing, close already occurred
					}
					@Override
					public String toString() {
						return "Closed MySQL session";
					}
				};
				closeRequest.setResult(null);
			}
			return closeRequest;
		} else {
			if (immediate) {
				logger.debug("Executing immediate close");
				// If the close is immediate, cancel pending requests and send request to server
				cancelPendingRequests(true);
				session.write(new CommandRequest(Command.QUIT));
				closeRequest = new Request<Void>() {
					@Override
					protected boolean cancelRequest(boolean mayInterruptIfRunning) {
						// Canceling is not possible when an immediate close
						return false;
					}
					@Override
					public void execute() throws Exception {
						// Do nothing, close was already sent
					}
					@Override
					public String toString() {
						return "MySQl immediate close";
					}
				};
			} else {
				// If the close is NOT immediate, schedule the close
				closeRequest = new Request<Void>() {
					@Override
					public boolean cancelRequest(boolean mayInterruptIfRunning) {
						logger.debug("Cancelling close");
						unclose();
						return true;
					}
					public synchronized void execute() {
						logger.debug("Sending QUIT to server");
						session.write(new CommandRequest(Command.QUIT));
					}
					@Override
					public String toString() {
						return "MySQL deferred close";
					}
				};
				enqueueRequest(closeRequest);
			}
		}
		logger.trace("Exiting close()");
		return closeRequest;
	}
	
	private synchronized void unclose() {
		logger.debug("Unclosing");
		this.closeRequest = null;
	}
	
	public synchronized boolean isClosed() {
		return closeRequest != null || session.isClosing();
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
			@Override
			public String toString() {
				return "SELECT request: " + sql;
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
			@Override
			public String toString() {
				return "MySQL update: " + sql;
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

	public synchronized Request<Void> getCloseRequest() {
		return closeRequest;
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
			throw new DbSessionClosedException(this, "This connection has been closed");
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
	public <E> void enqueueRequest(Request<E> request) {
		super.enqueueRequest(request);
	}
	
	/*
	 * Make this method public
	 */
	@Override
	public <E> Request<E> getActiveRequest() {
		return super.getActiveRequest();
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
