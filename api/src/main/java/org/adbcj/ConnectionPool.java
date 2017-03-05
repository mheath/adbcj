//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj;

import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ConnectionPool implements DbSessionProvider {

	private final Queue<DbSession> sessions = new ConcurrentLinkedQueue<DbSession>();
	private final ConnectionManager connectionManager;
	private final AtomicInteger count = new AtomicInteger(0);

	public ConnectionPool(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public void addConnection() {
		sessions.add(connectionManager.connect().getUninterruptably());
		count.incrementAndGet();
		System.out.println("Pool size at: " + count.get());
	}

	public void setPoolSize(int size) {
		if (size < count.get()) {
			throw new IllegalArgumentException("Can't decrease pool size.");
		}
		while (count.get() < size) {
			addConnection();
		}
	}

	
	public DbFuture<DbSession> connect() {
		final DbSession session = sessions.poll();
		if (session == null) {
			throw new IllegalStateException("No connections available in pool");
		}
		DefaultDbFuture<DbSession> future = new DefaultDbFuture<DbSession>();
		future.setResult(new DbSession() {

			
			public void beginTransaction() {
				session.beginTransaction();
			}

			
			public DbSessionFuture<Void> commit() {
				return session.commit();
			}

			
			public DbSessionFuture<Void> rollback() {
				return session.rollback();
			}

			
			public boolean isInTransaction() {
				return session.isInTransaction();
			}

			
			public DbSessionFuture<ResultSet> executeQuery(String sql) {
				return session.executeQuery(sql);
			}

			
			public <T> DbSessionFuture<T> executeQuery(String sql, ResultEventHandler<T> eventHandler, T accumulator) {
				return session.executeQuery(sql, eventHandler, accumulator);
			}

			
			public DbSessionFuture<Result> executeUpdate(String sql) {
				return session.executeUpdate(sql);
			}

			
			public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
				return session.prepareStatement(sql);
			}

			
			public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
				return session.prepareStatement(key, sql);
			}

			
			public DbSessionFuture<Void> close(boolean immediate) throws DbException {
				sessions.add(session);
				return DefaultDbSessionFuture.createCompletedFuture(this, null);
			}

			
			public boolean isClosed() throws DbException {
				return false;
			}
		});
		return future;
	}
}