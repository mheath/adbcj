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
package org.adbcj;

import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DbSessionPool implements DbSessionProvider {

	// Access must be synchronized on lock
	private List<ConnectionManagerEntry> connectionManagers = null;

	private Iterator<ConnectionManagerEntry> iterator;

	private final Object lock = this;

	public void addConnectionManager(ConnectionManager connectionManager, int connectionCount) {
		ConnectionManagerEntry entry = new ConnectionManagerEntry(connectionManager, connectionCount);
		synchronized (lock) {
			if (connectionManagers == null) {
				connectionManagers = new ArrayList<ConnectionManagerEntry>(1);
				connectionManagers.add(entry);
			} else {
				List<ConnectionManagerEntry> temp = new ArrayList<ConnectionManagerEntry>(connectionManagers.size() + 1);
				temp.addAll(connectionManagers);
				temp.add(entry);
				connectionManagers = temp;
			}
		}
	}

	public DbFuture<? extends DbSession> connect() {
		final DefaultDbFuture<DbSession> future = new DefaultDbFuture<DbSession>();
		future.setResult(new DbSession() {

			private volatile boolean closed = false;

			public void beginTransaction() {
				throw new IllegalStateException("Transactions not yet supported by connection pool");
			}

			public DbSessionFuture<Void> commit() {
				throw new IllegalStateException("Transactions not yet supported by connection pool");
			}

			public DbSessionFuture<Void> rollback() {
				throw new IllegalStateException("Transactions not yet supported by connection pool");
			}

			public boolean isInTransaction() {
				return false;
			}

			public DbSessionFuture<ResultSet> executeQuery(String sql) {
				return nextEntry().nextSession().executeQuery(sql);
			}

			public <T> DbSessionFuture<T> executeQuery(String sql, ResultEventHandler<T> eventHandler, T accumulator) {
				return nextEntry().nextSession().executeQuery(sql, eventHandler, accumulator);
			}

			public DbSessionFuture<Result> executeUpdate(String sql) {
				return nextEntry().nextSession().executeUpdate(sql);
			}

			public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
				return nextEntry().nextSession().prepareStatement(sql);
			}

			public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
				return nextEntry().nextSession().prepareStatement(key, sql);
			}

			public DbSessionFuture<Void> close(boolean immediate) throws DbException {
				closed = true;
				return DefaultDbSessionFuture.createCompletedFuture(this, null);
			}

			public boolean isClosed() throws DbException {
				return closed;
			}

			public boolean isPipeliningEnabled() {
				return false;
			}

			public void setPipeliningEnabled(boolean pipeliningEnabled) {
				throw new IllegalStateException("Can't change pipelining on pooled session");
			}
		});
		return future;
	}

	private ConnectionManagerEntry nextEntry() {
		synchronized (lock) {
			if (iterator == null || !iterator.hasNext()) {
				if (connectionManagers == null) {
					throw new DbException("No ConnectionManager's have been registered with this pool");
				}
				iterator = connectionManagers.iterator();
			}
			return iterator.next();
		}
	}

	private class ConnectionManagerEntry {
		// Access must be syncrhonized on entryLock
		private List<DbSession> sessions;

		private Iterator<DbSession> nextSession;

		private final Object entryLock = this;

		private final DbListener<Connection> connectionListener = new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> future) throws Exception {
				DbSession session = future.get();
				synchronized (entryLock) {
					if (sessions == null) {
						sessions = new LinkedList<DbSession>();
						sessions.add(session);
					} else {
						List<DbSession> temp = new LinkedList<DbSession>();
						temp.addAll(sessions);
						temp.add(session);
						sessions = temp;
					}
				}
			}
		};

		private ConnectionManagerEntry(ConnectionManager connectionManager, int connectionCount) {
			// Start all the connections
			// TODO This is not at all efficient, develop a better solution for this
			for (int i = 0; i < connectionCount; i++) {
				connectionManager.connect().addListener(connectionListener);
			}

		}

		private DbSession nextSession() {
			synchronized (entryLock) {
				if (nextSession == null || !nextSession.hasNext()) {
					if (sessions == null) {
						throw new IllegalStateException("No connections availabe");
					}
					nextSession = sessions.iterator();
				}
			}
			return nextSession.next();
		}
	}
}
