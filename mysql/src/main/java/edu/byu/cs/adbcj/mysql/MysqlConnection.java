package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.support.AbstractDbSessionFutureBase;
import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;

public class MysqlConnection implements Connection {

	private final ConnectionManager connectionManager;
	
	private final IoSession session;

	private final LoginCredentials credentials;
	private ServerGreeting serverGreeting;
	private State state = State.CONNECTING;

	private AbstractDbSessionFutureBase<Void> closeFuture;
	private volatile boolean closed = false;
	
	@SuppressWarnings("unchecked")
	private AbstractDbFutureListenerSupport currentFuture;
	
	public MysqlConnection(ConnectionManager connectionManager, IoSession session, LoginCredentials credentials) {
		this.connectionManager = connectionManager;
		this.session = session;
		this.credentials = credentials;
	}
	
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public synchronized DbSessionFuture<Void> close(final boolean immediate) throws DbException {
		if (closed) {
			return closeFuture;
		}
		if (closeFuture == null) {
			closeFuture = new AbstractDbSessionFutureBase<Void>(this) {
				@Override
				protected boolean doCancel(boolean mayInterruptIfRunning) {
					if (immediate) {
						return false;
					}
					// TODO Implement cancel
					// TODO If the cancel is successful, set the connection's closeFuture to null so that it can be recreated when close is called again
					return false;
				}
			};
			closed = true;
			if (immediate) {
				// TODO Cancel all pending futures
				session.write(new CommandRequest(Command.QUIT));
			} else {
				// TODO Schedule close
			}
		}
		
		return closeFuture;
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

	public DbSessionFuture<ResultSet> executeQuery(String sql) {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
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

	public synchronized AbstractDbSessionFutureBase<Void> getCloseFuture() {
		return closeFuture;
	}
	
	public LoginCredentials getCredentials() {
		return credentials;
	}

	public IoSession getIoSession() {
		return session;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	public MysqlCharacterSet getCharacterSet() {
		if (serverGreeting == null) {
			return MysqlCharacterSet.LATIN1_SWEDISH_CI;
		} else {
			return serverGreeting.getCharacterSet();
		}
	}
	
	@SuppressWarnings("unchecked")
	public AbstractDbFutureListenerSupport getCurrentFuture() {
		return currentFuture;
	}

	@SuppressWarnings("unchecked")
	public void setCurrentFuture(AbstractDbFutureListenerSupport currentFuture) {
		this.currentFuture = currentFuture;
	}
	
	private void checkClosed() {
		if (closed) {
			throw new DbException("This connection has been closed");
		}
	}

}
