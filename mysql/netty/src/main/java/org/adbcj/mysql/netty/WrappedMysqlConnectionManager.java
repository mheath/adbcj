package org.adbcj.mysql.netty;

import java.util.Properties;

import org.adbcj.Connection;
import org.adbcj.mysql.codec.AbstractMySqlConnectionManager;
import org.adbcj.mysql.netty.org.apache.commons.pool2.ObjectPool;
import org.adbcj.mysql.netty.org.apache.commons.pool2.PoolableObjectFactory;
import org.adbcj.mysql.netty.org.apache.commons.pool2.impl.GenericObjectPool;
import org.adbcj.mysql.netty.org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.adbcj.support.DefaultDbFuture;

public class WrappedMysqlConnectionManager extends AbstractMySqlConnectionManager {

    private MysqlConnectionManager  plainMySQLConnectionManager = null;
    private GenericObjectPoolConfig config                      = new GenericObjectPoolConfig();

    ObjectPool<Connection>          pool                        = null;

    public WrappedMysqlConnectionManager(String host, int port, String username, String password, String schema,
                                         Properties properties){
        super(username, password, schema, properties);
        this.plainMySQLConnectionManager = new MysqlConnectionManager(host,
            port,
            username,
            password,
            schema,
            properties);
        PoolableObjectFactory<Connection> factory = new WrappedMysqlConnectionPoolableObject(plainMySQLConnectionManager);
        config.setLifo(false);
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(1);
        config.setMaxWaitMillis(5 * 1000);
        pool = new GenericObjectPool<Connection>(factory, config);
    }

    public WrappedMysqlConnectionManager(String host, int port, String username, String password, String schema,
                                         Properties properties, GenericObjectPoolConfig config){
        super(username, password, schema, properties);
        this.plainMySQLConnectionManager = new MysqlConnectionManager(host,
            port,
            username,
            password,
            schema,
            properties);
        PoolableObjectFactory<Connection> factory = new WrappedMysqlConnectionPoolableObject(plainMySQLConnectionManager);
        pool = new GenericObjectPool<Connection>(factory, config);
    }

    @Override
    protected void dispose() {
        try {
            plainMySQLConnectionManager.dispose();

        } finally {
            pool.close();
        }
    }

    @Override
    protected DefaultDbFuture<Connection> createConnectionFuture() {
        DefaultDbFuture<Connection> dbFuture = new DefaultDbFuture<Connection>();
        try {
            dbFuture.setResult(new WrappedMysqlConnection(pool.borrowObject(), pool));
        } catch (Exception e) {
            dbFuture.setException(e);
        }
        return dbFuture;
    }

    public MysqlConnectionManager getPlainMySQLConnectionManager() {
        return plainMySQLConnectionManager;
    }

    public void setPlainMySQLConnectionManager(MysqlConnectionManager plainMySQLConnectionManager) {
        this.plainMySQLConnectionManager = plainMySQLConnectionManager;
    }

    public GenericObjectPoolConfig getConfig() {
        return config;
    }

    public void setConfig(GenericObjectPoolConfig config) {
        this.config = config;
    }

    public ObjectPool<Connection> getPool() {
        return pool;
    }

    public void setPool(ObjectPool<Connection> pool) {
        this.pool = pool;
    }

}
