package com.taobao.tdhs.config;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.adbcj.ConnectionManager;
import org.adbcj.mysql.netty.WrappedMysqlConnectionManager;
import org.adbcj.mysql.netty.org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;

/**
 * 数据库动态切换的Handle类，所有数据库的动态切换 都是由这个类完成
 * 
 * @author qihao
 */
public class TAtomDsConfHandle {

    private static Log             logger      = LogFactory.getLog(TAtomDsConfHandle.class);
    private String                 appName;

    private String                 dbKey;

    private ConnectionManager      connectionManager;
    /**
     * 运行时配置
     */
    private volatile TAtomDsConfDO runTimeConf = new TAtomDsConfDO();

    /**
     * 本地配置，优先于推送的动态配置
     */
    private TAtomDsConfDO          localConf   = new TAtomDsConfDO();

    /**
     * 全局配置，应用配置订阅管理
     */
    private DbConfManager          dbConfManager;

    /**
     * 密码配置订阅管理
     */
    private DbPasswdManager        dbPasswdManager;

    /**
     * 初始化标记为一但初始化过，所有本地的配置禁止改动
     */
    private volatile boolean       initFalg;

    /**
     * 数据源操作锁，当需要对数据源进行重建或者刷新时需要先获得该锁
     */
    private final ReentrantLock    lock        = new ReentrantLock();

    /**
     * 初始化方法，创建对应的数据源，只能被调用一次
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] double call Init !");
        }
        // 1.初始化参数检查
        if (StringUtil.isBlank(this.appName) || StringUtil.isBlank(this.dbKey)) {
            String errorMsg = "[attributeError] TAtomDatasource of appName Or dbKey is Empty !";
            logger.error(errorMsg);
            throw new AtomIllegalException(errorMsg);
        }
        // 2.配置dbConfManager
        DiamondDbConfManager defaultDbConfManager = new DiamondDbConfManager();
        defaultDbConfManager.setGlobalConfigDataId(TAtomConstants.getGlobalDataId(this.dbKey));
        defaultDbConfManager.setAppConfigDataId(TAtomConstants.getAppDataId(this.appName, this.dbKey));
        // 初始化dbConfManager
        defaultDbConfManager.init();
        dbConfManager = defaultDbConfManager;
        // 3.获取全局配置
        String globaConfStr = dbConfManager.getGlobalDbConf();
        // 注册全局配置监听
        registerGlobaDbConfListener(defaultDbConfManager);
        if (StringUtil.isBlank(globaConfStr)) {
            String errorMsg = "[ConfError] read globalConfig is Empty !";
            logger.error(errorMsg);
            throw new AtomInitialException(errorMsg);
        }
        // 4.获取应用配置
        String appConfStr = dbConfManager.getAppDbDbConf();
        // 注册应用配置监听
        registerAppDbConfListener(defaultDbConfManager);
        if (StringUtil.isBlank(appConfStr)) {
            String errorMsg = "[ConfError] read appConfig is Empty !";
            logger.error(errorMsg);
            throw new AtomInitialException(errorMsg);
        }
        lock.lock();
        try {
            // 5.解析配置string成TAtomDsConfDO
            runTimeConf = TAtomConfParser.parserTAtomDsConfDO(globaConfStr, appConfStr);
            // 6.处理本地优先配置
            overConfByLocal(localConf, runTimeConf);
            // 7.如果没有设置本地密码，则用订的密码，初始化passwdManager
            if (StringUtil.isBlank(this.runTimeConf.getPasswd())) {
                // 检查dbKey和对应的userName是否为空
                if (StringUtil.isBlank(runTimeConf.getUserName())) {
                    String errorMsg = "[attributeError] TAtomDatasource of UserName is Empty !";
                    logger.error(errorMsg);
                    throw new AtomIllegalException(errorMsg);
                }
                DiamondDbPasswdManager diamondDbPasswdManager = new DiamondDbPasswdManager();
                diamondDbPasswdManager.setPasswdConfDataId(TAtomConstants.getPasswdDataId(runTimeConf.getDbName(),
                    runTimeConf.getDbType(),
                    runTimeConf.getUserName()));
                diamondDbPasswdManager.init();
                dbPasswdManager = diamondDbPasswdManager;
                // 获取密码
                String passwd = dbPasswdManager.getPasswd();
                if (StringUtil.isBlank(passwd)) {
                    String errorMsg = "[PasswdError] read passwd is Empty !";
                    logger.error(errorMsg);
                    throw new AtomInitialException(errorMsg);
                }
                runTimeConf.setPasswd(passwd);
            }
            String ip = runTimeConf.getIp();

            String port = runTimeConf.getPort();
            // 因为是epoll模型，所以连接数减少一些

            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            int blockingTimeout = runTimeConf.getBlockingTimeout();
            if (blockingTimeout > -1) {
                config.setBlockWhenExhausted(true);
                config.setMaxWaitMillis(blockingTimeout);
            } else {
                config.setBlockWhenExhausted(false);
            }

            int maxPoolSize = runTimeConf.getMaxPoolSize();
            if (maxPoolSize > 0) {
                config.setMaxTotal(maxPoolSize);
            }
            int minPoolSize = runTimeConf.getMinPoolSize();
            if (minPoolSize > 0) {
                config.setMinIdle(minPoolSize);
            }
            config.setLifo(false);
            connectionManager = new WrappedMysqlConnectionManager(ip,
                Integer.valueOf(port),
                runTimeConf.getUserName(),
                runTimeConf.getPasswd(),
                runTimeConf.getDbName(),
                null,
                config);
            initFalg = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 全局配置监听,全局配置发生变化， 需要重新FLUSH数据源
     * 
     * @param defaultDbConfManager
     */
    private void registerGlobaDbConfListener(DbConfManager dbConfManager) {
        dbConfManager.registerGlobaDbConfListener(new ConfigDataListener() {

            public void onDataRecieved(String dataId, String data) {
                logger.error("[GlobaConf HandleData] dataId : " + dataId + " data: " + data);
                if (null == data || StringUtil.isBlank(data)) {
                    return;
                }
                lock.lock();
                try {
                } finally {
                    lock.unlock();
                }
            }

        });
    }

    /**
     * 应用配置监听，当应用配置发生变化时，区分发生 变化的配置，来决定具体是flush还是reCreate
     * 
     * @param defaultDbConfManager
     */
    private void registerAppDbConfListener(DbConfManager dbConfManager) {
        dbConfManager.registerAppDbConfListener(new ConfigDataListener() {

            public void onDataRecieved(String dataId, String data) {
                logger.error("[AppConf HandleData] dataId : " + dataId + " data: " + data);
                if (null == data || StringUtil.isBlank(data)) {
                    return;
                }
                lock.lock();
                try {
                    String appConfStr = data;
                    TAtomDsConfDO tmpConf = TAtomConfParser.parserTAtomDsConfDO(null, appConfStr);
                    TAtomDsConfDO newConf = TAtomDsConfHandle.this.runTimeConf.clone();
                    // 有些既有配置不能变更，所以克隆老的配置，然后将新的set进去
                    newConf.setUserName(tmpConf.getUserName());
                    newConf.setMinPoolSize(tmpConf.getMinPoolSize());
                    newConf.setMaxPoolSize(tmpConf.getMaxPoolSize());
                    newConf.setIdleTimeout(tmpConf.getIdleTimeout());
                    newConf.setBlockingTimeout(tmpConf.getBlockingTimeout());
                    newConf.setPreparedStatementCacheSize(tmpConf.getPreparedStatementCacheSize());
                    newConf.setConnectionProperties(tmpConf.getConnectionProperties());
                    newConf.setOracleConType(tmpConf.getOracleConType());
                    // 增加3个具体的实现
                    newConf.setWriteRestrictTimes(tmpConf.getWriteRestrictTimes());
                    newConf.setReadRestrictTimes(tmpConf.getReadRestrictTimes());
                    newConf.setThreadCountRestrict(tmpConf.getThreadCountRestrict());
                    newConf.setTimeSliceInMillis(tmpConf.getTimeSliceInMillis());
                    // 处理本地优先配置
                    overConfByLocal(TAtomDsConfHandle.this.localConf, newConf);
                    // 转换tAtomDsConfDO
                    // 检查转换后结果是否正确
                } finally {
                    lock.unlock();
                }
            }

        });
    }

    /**
     * 是用本地配置覆盖传入的TAtomDsConfDO的属性
     * 
     * @param tAtomDsConfDO
     */
    private void overConfByLocal(TAtomDsConfDO localDsConfDO, TAtomDsConfDO newDsConfDO) {
        if (null == newDsConfDO || null == localDsConfDO) {
            return;
        }
        if (StringUtil.isNotBlank(localDsConfDO.getDriverClass())) {
            newDsConfDO.setDriverClass(localDsConfDO.getDriverClass());
        }
        if (StringUtil.isNotBlank(localDsConfDO.getSorterClass())) {
            newDsConfDO.setSorterClass(localDsConfDO.getSorterClass());
        }
        if (StringUtil.isNotBlank(localDsConfDO.getPasswd())) {
            newDsConfDO.setPasswd(localDsConfDO.getPasswd());
        }
        if (null != localDsConfDO.getConnectionProperties() && !localDsConfDO.getConnectionProperties().isEmpty()) {
            newDsConfDO.setConnectionProperties(localDsConfDO.getConnectionProperties());
        }
    }

    void setSingleInGroup(boolean isSingleInGroup) {
        this.runTimeConf.setSingleInGroup(isSingleInGroup);
    }

    public void setAppName(String appName) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset appName !");
        }
        this.appName = appName;
    }

    public void setDbKey(String dbKey) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset dbKey !");
        }
        this.dbKey = dbKey;
    }

    public void setLocalPasswd(String passwd) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset passwd !");
        }
        this.localConf.setPasswd(passwd);
    }

    public void setLocalConnectionProperties(Map<String, String> map) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset connectionProperties !");
        }
        this.localConf.setConnectionProperties(map);
    }

    public void setLocalDriverClass(String driverClass) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset driverClass !");
        }
        this.localConf.setDriverClass(driverClass);
    }

    public void setLocalSorterClass(String sorterClass) throws AtomAlreadyInitException {
        if (initFalg) {
            throw new AtomAlreadyInitException("[AlreadyInit] couldn't Reset sorterClass !");
        }
        this.localConf.setSorterClass(sorterClass);
    }

    public String getAppName() {
        return appName;
    }

    public String getDbKey() {
        return dbKey;
    }

    public AtomDbStatusEnum getStatus() {
        return this.runTimeConf.getDbStautsEnum();
    }

    public AtomDbTypeEnum getDbType() {
        return this.runTimeConf.getDbTypeEnum();
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

}
