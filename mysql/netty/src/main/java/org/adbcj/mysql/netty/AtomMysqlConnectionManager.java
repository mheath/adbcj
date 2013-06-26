package org.adbcj.mysql.netty;

import java.util.HashMap;
import java.util.Map;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tdhs.config.AtomAlreadyInitException;
import com.taobao.tdhs.config.TAtomDsConfHandle;

public class AtomMysqlConnectionManager implements ConnectionManager {

    private static Map<String, TAtomDsConfHandle> cacheConfHandleMap = new HashMap<String, TAtomDsConfHandle>();

    private final static Log                      logger             = LogFactory.getLog(AtomMysqlConnectionManager.class);
    TAtomDsConfHandle                             atomTdhs           = new TAtomDsConfHandle();
    protected volatile boolean                    inited             = false;

    public void setAppName(String appName) throws AtomAlreadyInitException {
        atomTdhs.setAppName(appName);
    }

    public void setDbKey(String dbKey) throws AtomAlreadyInitException {
        atomTdhs.setDbKey(dbKey);
    }

    public String getAppName() {
        return atomTdhs.getAppName();
    }

    public String getDbKey() {
        return atomTdhs.getDbKey();
    }

    public void init() {
        if (inited) {
            return;
        }
        inited = true;
        try {
            String dbName = TAtomConstants.getDbNameStr(this.getAppName(), this.getDbKey());
            synchronized (cacheConfHandleMap) {
                TAtomDsConfHandle cacheConfHandle = cacheConfHandleMap.get(dbName);
                if (null == cacheConfHandle) {
                    // 初始化config的管理器
                    this.atomTdhs.init();
                    cacheConfHandleMap.put(dbName, atomTdhs);
                    logger.info("create new TAtomDsConfHandle dbName : " + dbName);
                } else {
                    atomTdhs = cacheConfHandle;
                    logger.info("use the cache TAtomDsConfHandle dbName : " + dbName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DbFuture<Connection> connect() {
        init();
        return atomTdhs.getConnectionManager().connect();
    }

    public DbFuture<Void> close(boolean immediate) throws DbException {
        init();
        return atomTdhs.getConnectionManager().close(immediate);
    }

    public boolean isClosed() {
        init();
        return atomTdhs.getConnectionManager().isClosed();
    }

    public boolean isPipeliningEnabled() {
        init();
        return atomTdhs.getConnectionManager().isPipeliningEnabled();
    }

    public void setPipeliningEnabled(boolean pipeliningEnabled) {
        init();
        atomTdhs.getConnectionManager().setPipeliningEnabled(pipeliningEnabled);
    }
}
