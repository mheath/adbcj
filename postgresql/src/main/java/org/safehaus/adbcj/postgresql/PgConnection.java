package org.safehaus.adbcj.postgresql;

import java.nio.charset.Charset;

import org.apache.mina.common.IoSession;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbSessionFuture;
import org.safehaus.adbcj.PreparedStatement;
import org.safehaus.adbcj.Result;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.TransactionIsolationLevel;
import org.safehaus.adbcj.support.AbstractTransactionalSession;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.Request;

public class PgConnection extends AbstractTransactionalSession implements Connection {

	private final PgConnectionManager connectionManager;
	private final IoSession session;
	private final Charset frontendCharset = Charset.forName("UTF-8"); // TODO Make charset configurable
	private final Charset backendCharset = Charset.forName("US-ASCII");

	private DefaultDbFuture<Connection> connectFuture;
	private DefaultDbFuture<Void> closeFuture;
	
	public PgConnection(PgConnectionManager connectionManager, IoSession session, DefaultDbFuture<Connection> connectFuture) {
		this.connectionManager = connectionManager;
		this.session = session;
		this.connectFuture = connectFuture;
	}
	
	@Override
	protected void checkClosed() {
		// TODO Implement PgConnection.checkClosed()
		throw new IllegalStateException();
	}

	@Override
	protected DbSessionFuture<Void> enqueueChangeIsolationLevel(Transaction transaction,
			TransactionIsolationLevel transactionIsolationLevel) {
		// TODO Implement enqueueChangeIsolationLevel
		throw new IllegalStateException();
	}

	@Override
	protected DbSessionFuture<Void> enqueueCommit(Transaction transaction) {
		// TODO Implement enqueueChangeIsolationLevel
		throw new IllegalStateException();
	}

	@Override
	protected DbSessionFuture<Void> enqueueRollback(Transaction transaction) {
		// TODO Implement enqueueChangeIsolationLevel
		throw new IllegalStateException();
	}

	@Override
	protected DbSessionFuture<Void> enqueueStartTransaction(Transaction transaction) {
		// TODO Implement enqueueChangeIsolationLevel
		throw new IllegalStateException();
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public DbFuture<Void> ping() {
		// TODO Implement ping
		throw new IllegalStateException();
	}

	public DbSessionFuture<Void> close(boolean immediate) throws DbException {
		// TODO Implement close
		throw new IllegalStateException();
	}

	public DbSessionFuture<ResultSet> executeQuery(String sql) {
		// TODO Implement executeQuery
		throw new IllegalStateException();
	}

	public DbSessionFuture<Result> executeUpdate(String sql) {
		// TODO Implement executeUpdate
		throw new IllegalStateException();
	}

	public boolean isClosed() throws DbException {
		// TODO Implement isClosed
		throw new IllegalStateException();
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		// TODO Implement prepareStatement
		throw new IllegalStateException();
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
		// TODO Implement prepareStatement
		throw new IllegalStateException();
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

	public DefaultDbFuture<?> getActiveFuture() {
		if (connectFuture != null) {
			return connectFuture;
		}
		Request<?> request = getActiveRequest();
		if (request != null && request.getFuture() != null) {
			return request.getFuture();
		}
		return closeFuture;
	}

	public void clearConnectFuture() {
		this.connectFuture = null;
	}
	
}
