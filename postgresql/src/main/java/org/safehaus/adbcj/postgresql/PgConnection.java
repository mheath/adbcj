package org.safehaus.adbcj.postgresql;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.IoSession;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbSessionFuture;
import org.safehaus.adbcj.PreparedStatement;
import org.safehaus.adbcj.Result;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.postgresql.frontend.BindMessage;
import org.safehaus.adbcj.postgresql.frontend.DescribeMessage;
import org.safehaus.adbcj.postgresql.frontend.ExecuteMessage;
import org.safehaus.adbcj.postgresql.frontend.FrontendMessage;
import org.safehaus.adbcj.postgresql.frontend.ParseMessage;
import org.safehaus.adbcj.support.AbstractTransactionalSession;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.DefaultDbSessionFuture;
import org.safehaus.adbcj.support.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgConnection extends AbstractTransactionalSession implements Connection {
	
	private final Logger logger = LoggerFactory.getLogger(PgConnection.class);

	private final PgConnectionManager connectionManager;
	private final IoSession session;
	private final Charset frontendCharset = Charset.forName("UTF-8"); // TODO Make charset configurable
	private final Charset backendCharset = Charset.forName("US-ASCII");

	private DefaultDbFuture<Connection> connectFuture;
	private DefaultDbSessionFuture<Void> closeFuture;

	// Constant Messages
	private static final ExecuteMessage DEFAULT_EXECUTE = new ExecuteMessage();
	private static final BindMessage DEFAULT_BIND = new BindMessage();
	private static final DescribeMessage DEFAULT_DESCRIBE = DescribeMessage.createDescribePortalMessage(null);
	
	public PgConnection(PgConnectionManager connectionManager, IoSession session, DefaultDbFuture<Connection> connectFuture) {
		this.connectionManager = connectionManager;
		this.session = session;
		this.connectFuture = connectFuture;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public DbFuture<Void> ping() {
		// TODO Implement ping
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
				closeFuture = new DefaultDbSessionFuture<Void>(this);
				closeFuture.setDone();
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
						close(true);
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

	public DbSessionFuture<ResultSet> executeQuery(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<ResultSet>() {
			@Override
			public void execute() throws Exception {
				logger.debug("Issuing query: {}", sql);
				
				ParseMessage parse = new ParseMessage(sql);
				// TODO We need to come up with a way to batch writes so that we're not sending lots of small packets
				session.write(parse);
				session.write(DEFAULT_BIND);
				session.write(DEFAULT_DESCRIBE);
				session.write(DEFAULT_EXECUTE);
				session.write(FrontendMessage.SYNC);
			}
			
			@Override
			public String toString() {
				return "Postgresql execute query request: " + sql; 
			}
		});
	}

	public DbSessionFuture<Result> executeUpdate(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<Result>() {
			@Override
			public void execute() throws Exception {
				logger.debug("Issuing update query: {}", sql);
				
				ParseMessage parse = new ParseMessage(sql);
				// TODO We need to come up with a way to batch writes so that we're not sending lots of small packets
				session.write(parse);
				session.write(DEFAULT_BIND);
				session.write(DEFAULT_DESCRIBE);
				session.write(DEFAULT_EXECUTE);
				session.write(FrontendMessage.SYNC);
			}
			
			@Override
			public String toString() {
				return "Postgresql execute update request: " + sql; 
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
		BindMessage bind = new BindMessage(statementId);
		session.write(bind);
		session.write(DEFAULT_EXECUTE);
		session.write(FrontendMessage.SYNC);
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
	
	public DefaultDbFuture<Connection> getConnectFuture() {
		return connectFuture;
	}
	
	public void clearConnectFuture() {
		this.connectFuture = null;
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
	
}
