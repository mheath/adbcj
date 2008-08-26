package org.adbcj.mysql.codec;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.support.DefaultDbFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMySqlConnectionManager implements
		ConnectionManager {
	private static final Logger logger = LoggerFactory.getLogger(AbstractMySqlConnectionManager.class);

	private final LoginCredentials credentials;

	private final AtomicInteger id = new AtomicInteger();
	private final Set<AbstractMySqlConnection> connections = new HashSet<AbstractMySqlConnection>();

	private DbFuture<Void> closeFuture = null;

	private volatile boolean pipeliningEnabled = true;

	public AbstractMySqlConnectionManager(String username, String password, String schema, Properties properties) {
		this.credentials = new LoginCredentials(username, password, schema);
	}

	public synchronized DbFuture<Void> close(boolean immediate) throws DbException {
		if (isClosed()) {
			return closeFuture;
		}
		// TODO: Close all open connections
		if (immediate) {
			dispose();
			DefaultDbFuture<Void> future = new DefaultDbFuture<Void>();
			future.setResult(null);
			closeFuture = future;
			return closeFuture;
		} else {
			// TODO In MysqlConnectionManager.close() implement deferred close
			throw new IllegalStateException("Deferred close not yet implemented");
		}
	}

	public synchronized boolean isClosed() {
		return closeFuture != null;
	}

	public DbFuture<Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager closed");
		}
		logger.debug("Starting connection");

		return createConnectionFuture();
	}

	protected abstract void dispose();

	protected abstract DefaultDbFuture<Connection> createConnectionFuture();

	public boolean isPipeliningEnabled() {
		return pipeliningEnabled;
	}

	public void setPipeliningEnabled(boolean pipeliningEnabled) {
		this.pipeliningEnabled = pipeliningEnabled;
	}

	protected void addConnection(AbstractMySqlConnection connection) {
		synchronized (connections) {
			connections.add(connection);
		}
	}

	protected boolean removeConnection(AbstractMySqlConnection connection) {
		synchronized (connections) {
			return connections.remove(connection);
		}
	}

	public int nextId() {
		return id.incrementAndGet();
	}

	protected LoginCredentials getCredentials() {
		return credentials;
	}

}
