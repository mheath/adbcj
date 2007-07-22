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
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;
import edu.byu.cs.adbcj.support.AbstractDbSessionFutureBase;
import edu.byu.cs.adbcj.support.BaseRequestQueue;
import edu.byu.cs.adbcj.support.DbSessionFutureProxy;
import edu.byu.cs.adbcj.support.RequestAction;

public class MysqlConnection extends BaseRequestQueue implements Connection {

	private final ConnectionManager connectionManager;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;

	private AbstractDbFutureBase<Void> closeFuture;
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
				closeFuture = new AbstractDbSessionFutureBase<Void>(this) {
					@Override
					protected boolean doCancel(boolean mayInterruptIfRunning) {
						// Canceling is not possible when an immediate close
						return false;
					}
				};
			} else {
				// If the close is NOT immediate, schedule the close
				closeFuture = enqueueRequest(new RequestAction() {
					private boolean closed = false;
					public synchronized boolean cancle(boolean mayInterruptIfRunning) {
						if (!closed) {
							unclose();
							return true;
						}
						return false;
					}
					public synchronized void execute() {
						closed = true;
						// Do a close immediate to close the connection
						close(true);
					}
				});
			}
		}
			
		return newFutureProxy(closeFuture);
	}
	
	private synchronized void unclose() {
		this.closeFuture = null;
		closed = false;
	}
	
	public boolean isClosed() {
		return closed;
	}

	public void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel) {
		checkClosed();
		// TODO Auto-generated method stub

	}

	public TransactionIsolationLevel getTransactionIsolationLevel() {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	public void beginTransaction() {
		checkClosed();
		// TODO Auto-generated method stub
	}

	public boolean isInTransaction() {
		checkClosed();
		// TODO Auto-generated method stub
		return false;
	}

	public DbSessionFuture<Void> commit() {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<Void> rollback() {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<ResultSet> executeQuery(final String sql) {
		checkClosed();
		DbFuture<?> future = enqueueRequest(new RequestAction() {
			public boolean cancle(boolean mayInterruptIfRunning) {
				return false;
			}
			public void execute() {
				CommandRequest request = new CommandRequest(Command.QUERY, sql);
				session.write(request);
			}
		});
		return newFutureProxy(future);
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
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

	public synchronized AbstractDbFutureBase<Void> getCloseFuture() {
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

	@SuppressWarnings("unchecked")
	private <E> DbSessionFuture<E> newFutureProxy(DbFuture<?> future) {
		return new DbSessionFutureProxy<E>((DbFuture<E>)future, this);
	}

	//
	//
	// Queuing methods
	//
	//
	
	@Override
	public synchronized <E> AbstractDbFutureBase<E> enqueueRequest(RequestAction action) {
		return super.enqueueRequest(action);
	}
	
	@Override
	public Request getActiveRequest() {
		return super.getActiveRequest();
	}
	
	@Override
	public synchronized Request makeNextRequestActive() {
		return super.makeNextRequestActive();
	}
}
