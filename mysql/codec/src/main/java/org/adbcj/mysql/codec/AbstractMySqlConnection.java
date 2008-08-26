package org.adbcj.mysql.codec;

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
import org.adbcj.support.AbstractDbSession;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.AbstractDbSession.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMySqlConnection extends AbstractDbSession implements Connection {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMySqlConnection.class);

	private final AbstractMySqlConnectionManager connectionManager;

	private final int id;

	private final LoginCredentials credentials;

	private Request<Void> closeRequest;

	private MysqlCharacterSet charset = MysqlCharacterSet.LATIN1_SWEDISH_CI;

	protected AbstractMySqlConnection(AbstractMySqlConnectionManager connectionManager, LoginCredentials credentials) {
		super(connectionManager.isPipeliningEnabled());
		this.connectionManager = connectionManager;
		this.credentials = credentials;
		this.id = connectionManager.nextId();
		connectionManager.addConnection(this);
	}

	public abstract void write(ClientRequest request);

	protected abstract boolean isTransportClosing();

	public abstract DefaultDbFuture<Connection> getConnectFuture();

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
						return "Close MySQL session";
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
				write(new CommandRequest(Command.QUIT));
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
						write(new CommandRequest(Command.QUIT));
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
		return closeRequest != null || isTransportClosing();
	}

	public <T> DbSessionFuture<T> executeQuery(final String sql, ResultEventHandler<T> eventHandler, T accumulator) {
		checkClosed();
		return enqueueTransactionalRequest(new Request<T>(eventHandler, accumulator) {
			@Override
			public void execute() throws Exception {
				logger.debug("Sending query '{}'", sql);
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				write(request);
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
				write(request);
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
		write(COMMIT);
	}

	@Override
	protected void sendRollback() {
		write(ROLLBACK);
	}

	@Override
	protected void sendBegin() {
		write(BEGIN);
	}

	// ************* Non-API methods *************************************************************

	public synchronized Request<Void> getCloseRequest() {
		return closeRequest;
	}

	public LoginCredentials getCredentials() {
		return credentials;
	}

	public MysqlCharacterSet getCharacterSet() {
		return charset;
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
		if (!(obj.getClass() == getClass())) {
			return false;
		}
		return id == ((AbstractMySqlConnection)obj).id;
	}

	public void doClose() {
		connectionManager.removeConnection(this);

		Request<Void> closeRequest = getCloseRequest();
		if (closeRequest != null) {
			closeRequest.complete(null);
		}
		// TODO Make a DbSessionClosedException and use here
		errorPendingRequests(new DbException("Connection closed"));
	}


}
