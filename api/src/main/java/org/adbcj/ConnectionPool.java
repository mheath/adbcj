//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultDbSessionFuture;

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

    @Override
    public DbFuture<DbSession> connect() {
        final DbSession session = sessions.poll();
        if (session == null) {
            throw new IllegalStateException("No connections available in pool");
        }
        DefaultDbFuture<DbSession> future = new DefaultDbFuture<DbSession>();
        future.setResult(new DbSession() {

            @Override
            public void beginTransaction() {
                session.beginTransaction();
            }

            @Override
            public DbSessionFuture<Void> commit() {
                return session.commit();
            }

            @Override
            public DbSessionFuture<Void> rollback() {
                return session.rollback();
            }

            @Override
            public boolean isInTransaction() {
                return session.isInTransaction();
            }

            @Override
            public DbSessionFuture<ResultSet> executeQuery(String sql) {
                return session.executeQuery(sql);
            }

            @Override
            public <T> DbSessionFuture<T> executeQuery(String sql, ResultEventHandler<T> eventHandler, T accumulator) {
                return session.executeQuery(sql, eventHandler, accumulator);
            }

            @Override
            public DbSessionFuture<Result> executeUpdate(String sql) {
                return session.executeUpdate(sql);
            }

            @Override
            public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
                return session.prepareStatement(sql);
            }

            @Override
            public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
                return session.prepareStatement(key, sql);
            }

            @Override
            public DbSessionFuture<Void> close(boolean immediate) throws DbException {
                sessions.add(session);
                return DefaultDbSessionFuture.createCompletedFuture(this, null);
            }

            @Override
            public boolean isClosed() throws DbException {
                return false;
            }
        });
        return future;
    }
}