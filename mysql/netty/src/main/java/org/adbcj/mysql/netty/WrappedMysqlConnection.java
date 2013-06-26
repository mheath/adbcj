package org.adbcj.mysql.netty;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbSessionFuture;
import org.adbcj.PreparedStatement;
import org.adbcj.Result;
import org.adbcj.ResultEventHandler;
import org.adbcj.ResultSet;
import org.adbcj.mysql.netty.org.apache.commons.pool2.ObjectPool;
import org.adbcj.support.DefaultDbSessionFuture;

public class WrappedMysqlConnection implements Connection {

    protected final ObjectPool<Connection> pool;
    protected final Connection             conn;

    public WrappedMysqlConnection(Connection conn, ObjectPool<Connection> pool){
        this.pool = pool;
        this.conn = conn;
    }

    public synchronized DbSessionFuture<Void> close(boolean immediate) throws DbException {
        try {
            pool.returnObject(conn);
        } catch (Exception e) {
            throw new DbException(e);
        }
        return new DefaultDbSessionFuture<Void>(null);
    }

    public void beginTransaction() {
        conn.beginTransaction();
    }

    public DbSessionFuture<Void> commit() {
        return conn.commit();
    }

    public ConnectionManager getConnectionManager() {
        return conn.getConnectionManager();
    }

    public DbFuture<Void> ping() {
        return conn.ping();
    }

    public DbSessionFuture<Void> rollback() {
        return conn.rollback();
    }

    public boolean isInTransaction() {
        return conn.isInTransaction();
    }

    public DbSessionFuture<ResultSet> executeQuery(String sql) {
        return conn.executeQuery(sql);
    }

    public <T> DbSessionFuture<T> executeQuery(String sql, ResultEventHandler<T> eventHandler, T accumulator) {
        return conn.executeQuery(sql, eventHandler, accumulator);
    }

    public DbSessionFuture<Result> executeUpdate(String sql) {
        return conn.executeUpdate(sql);
    }

    public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
        return conn.prepareStatement(sql);
    }

    public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
        return conn.prepareStatement(key, sql);
    }

    public boolean isClosed() throws DbException {
        return conn.isClosed();
    }

}
