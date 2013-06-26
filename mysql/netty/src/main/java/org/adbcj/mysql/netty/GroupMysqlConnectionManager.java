package org.adbcj.mysql.netty;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;

import com.taobao.tdhs.config.group.ConfigManager;
import com.taobao.tdhs.config.group.WeightSelector;

/**
 * TODO !! 目前只允许写主机。。 简略一点为了先能跑通
 * http://gitlab.alibaba-inc.com/shenxun/adbjc-tb/issues/2785
 * 
 * @author Whisper 2013-6-21 下午3:12:42
 * @since 3.0.1
 */
public class GroupMysqlConnectionManager implements ConnectionManager {

    private static String      VERSION        = "2.4.1";
    private static String      PREFIX         = "com.taobao.tddl.jdbc.group_V" + VERSION + "_";
    private String             dbGroupKey;
    private String             fullDbGroupKey = null;
    private String             appName;
    private ConfigManager      configManager  = new ConfigManager();
    protected volatile boolean inited         = false;

    public void init() {
        if (inited) {
            return;
        }
        inited = true;
        configManager.setAppName(appName);
        configManager.setFullGroupKey(getFullDbGroupKey());
        configManager.init();
        // db1:rwp0q0i1
    }

    public GroupMysqlConnectionManager(String dbGroupKey, String appName){
        super();
        this.dbGroupKey = dbGroupKey;
        this.appName = appName;
    }

    public ConnectionManager getClient(boolean isWrite) {
        int index = 0;
        if (isWrite) {
            WeightSelector ws = configManager.getWriteSelector();
            index = ws.getIndex();

        } else {
            WeightSelector ws = configManager.getReadSelector();
            index = ws.getIndex();
        }
        return configManager.getAtomTdhsClient().get(index);
    }

    public static String getFullDbGroupKey(String dbGroupKey) {
        return PREFIX + dbGroupKey;
    }

    public String getFullDbGroupKey() {
        if (fullDbGroupKey == null) fullDbGroupKey = PREFIX + getDbGroupKey();
        return fullDbGroupKey;
    }

    public String getDbGroupKey() {
        return dbGroupKey;
    }

    public void setDbGroupKey(String dbGroupKey) {
        this.dbGroupKey = dbGroupKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public DbFuture<Connection> connect() {
        init();
        return getClient(true).connect();
    }

    public DbFuture<Void> close(boolean immediate) throws DbException {
        init();
        return getClient(true).close(immediate);
    }

    public boolean isClosed() {
        init();
        return getClient(true).isClosed();
    }

    public boolean isPipeliningEnabled() {
        init();
        return getClient(true).isPipeliningEnabled();
    }

    public void setPipeliningEnabled(boolean pipeliningEnabled) {
        init();
        getClient(true).setPipeliningEnabled(pipeliningEnabled);
    }

}
