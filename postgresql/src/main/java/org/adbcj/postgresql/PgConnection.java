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
package org.adbcj.postgresql;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.IoSession;
import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbSessionFuture;
import org.adbcj.PreparedStatement;
import org.adbcj.Result;
import org.adbcj.ResultEventHandler;
import org.adbcj.support.AbstractTransactionalSession;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;
import org.adbcj.postgresql.PgConnectionManager.PgConnectFuture;
import org.adbcj.postgresql.frontend.AbstractFrontendMessage;
import org.adbcj.postgresql.frontend.BindMessage;
import org.adbcj.postgresql.frontend.DescribeMessage;
import org.adbcj.postgresql.frontend.ExecuteMessage;
import org.adbcj.postgresql.frontend.FrontendMessage;
import org.adbcj.postgresql.frontend.ParseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgConnection extends AbstractTransactionalSession implements Connection {
	
	private final Logger logger = LoggerFactory.getLogger(PgConnection.class);

	private final PgConnectionManager connectionManager;
	private final PgConnectFuture connectFuture;
	private final IoSession session;
	// TODO Determine if we really need to distinguish frontend and backend charsets
	// TODO Make charset configurable
	private final Charset frontendCharset = Charset.forName("UTF-8");
	// TODO Update backendCharset based on what backend returns
	private Charset backendCharset = Charset.forName("US-ASCII");

	private DefaultDbSessionFuture<Void> closeFuture;

	private int pid;
	private int key;
	
	// Constant Messages
	private static final ExecuteMessage DEFAULT_EXECUTE = new ExecuteMessage();
	private static final BindMessage DEFAULT_BIND = new BindMessage();
	private static final DescribeMessage DEFAULT_DESCRIBE = DescribeMessage.createDescribePortalMessage(null);
	
	public PgConnection(PgConnectionManager connectionManager, PgConnectFuture connectFuture, IoSession session) {
		this.connectionManager = connectionManager;
		this.connectFuture = connectFuture;
		this.session = session;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public DbFuture<Void> ping() {
		// TODO Implement Postgresql ping
		throw new IllegalStateException();
	}

	@Override
	protected void checkClosed() {
		if (isClosed()) {
			throw new DbException(this, "This connection has been closed");
		}
	}

	public synchronized DbSessionFuture<Void> close(boolean immediate) throws DbException {
		// TODO PgConnection.close(boolean) is almost identical to MySQL close method, generify this
		
		// If the connection is already closed, return existing close future
		if (isClosed()) {
			if (closeFuture == null) {
				return DefaultDbSessionFuture.createCompletedFuture(this, null);
			}
		} else {
			if (immediate) {
				logger.debug("Executing immediate close");
				// If the close is immediate, cancel pending requests and send request to server
				cancelPendingRequests(true);
				session.write(FrontendMessage.TERMINATE);
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
						// Do a close immediate to close the connection
						session.write(FrontendMessage.TERMINATE);
					}
				});
			}
		}
		return closeFuture;
	}

	private synchronized void unclose() {
		logger.debug("Unclosing");
		this.closeFuture = null;
	}
	
	public boolean isClosed() throws DbException {
		return closeFuture != null || session.isClosing();
	}
	
	public <T> DbSessionFuture<T> executeQuery(final String sql, ResultEventHandler<T> eventHandler, T accumulator) {
		checkClosed();
		Request<T> request = new Request<T>(eventHandler, accumulator) {
			@Override
			public void execute() throws Exception {
				logger.debug("Issuing query: {}", sql);
				
				ParseMessage parse = new ParseMessage(sql);
				session.write(new AbstractFrontendMessage[] {
					parse,
					DEFAULT_BIND,
					DEFAULT_DESCRIBE,
					DEFAULT_EXECUTE,
					FrontendMessage.SYNC,
				});
			}
			@Override
			public String toString() {
				return "Select request: " + sql;
			}
		};
		return enqueueTransactionalRequest(request);
	}

	public DbSessionFuture<Result> executeUpdate(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<Result>() {
			@Override
			public void execute() throws Exception {
				logger.debug("Issuing update query: {}", sql);
				
				ParseMessage parse = new ParseMessage(sql);
				session.write(new AbstractFrontendMessage[] {
					parse,
					DEFAULT_BIND,
					DEFAULT_DESCRIBE,
					DEFAULT_EXECUTE,
					FrontendMessage.SYNC
				});
			}
			
			@Override
			public String toString() {
				return "Update request: " + sql; 
			}
		});
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		// TODO Implement prepareStatement
		throw new IllegalStateException();
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
		// TODO Implement prepareStatement
		throw new IllegalStateException();
	}
	
	// ******** Transaction methods ***********************************************************************************
	
	private final AtomicLong statementCounter = new AtomicLong();
	private Map<String, String> statementCache = new HashMap<String, String>();
	
	@Override
	protected void sendBegin() {
		executeStatement("BEGIN");
	}
	
	@Override
	protected void sendCommit() {
		executeStatement("COMMIT");
	}
	
	@Override
	protected void sendRollback() {
		executeStatement("ROLLBACK");
	}
	
	private synchronized void executeStatement(String statement) {
		String statementId = statementCache.get(statement);
		if (statementId == null) {
			long id = statementCounter.incrementAndGet();
			statementId = "S_" + id;
			
			ParseMessage parseMessage = new ParseMessage(statement, statementId);
			session.write(parseMessage);

			statementCache.put(statement, statementId);
		}
		session.write(new AbstractFrontendMessage[] {
				new BindMessage(statementId),
				DEFAULT_EXECUTE,
				FrontendMessage.SYNC
		});
	}
	
	// ================================================================================================================
	//
	// Non-API methods
	//
	// ================================================================================================================
	
	public Charset getFrontendCharset() {
		return frontendCharset;
	}
	
	public Charset getBackendCharset() {
		return backendCharset;
	}

	public DefaultDbFuture<Void> getCloseFuture() {
		return closeFuture;
	}
	
	public PgConnectFuture getConnectFuture() {
		return connectFuture;
	}
	
	@Override
	protected synchronized <E> DefaultDbSessionFuture<E> enqueueRequest(Request<E> request) {
		return super.enqueueRequest(request);
	}
	
	@Override
	public <E> Request<E> getActiveRequest() {
		return super.getActiveRequest();
	}
	
	@Override
	protected synchronized <E> Request<E> makeNextRequestActive() {
		Request<Object> request = getActiveRequest();
		if (request != null && !request.getFuture().isDone()) {
			throw new RuntimeException("Going to next request when current request isn't complete");
		}
		Request<E> activeRequest = super.makeNextRequestActive();
		return activeRequest;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}
	
}
