package com.taobao.tdhs.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.common.lang.StringUtil;
import com.alibaba.common.lang.io.ByteArrayInputStream;

/**
 * TAtom数据源的推送配置解析类
 * 
 * @author qihao
 */
public class TAtomConfParser {

    private static Log         logger                                = LogFactory.getLog(TAtomConfParser.class);

    public static final String GLOBA_IP_KEY                          = "ip";
    public static final String GLOBA_PORT_KEY                        = "port";
    public static final String GLOBA_DB_NAME_KEY                     = "dbName";
    public static final String GLOBA_DB_TYPE_KEY                     = "dbType";
    public static final String GLOBA_DB_STATUS_KEY                   = "dbStatus";
    public static final String APP_USER_NAME_KEY                     = "userName";
    public static final String APP_MIN_POOL_SIZE_KEY                 = "minPoolSize";
    public static final String APP_MAX_POOL_SIZE_KEY                 = "maxPoolSize";
    public static final String APP_IDLE_TIMEOUT_KEY                  = "idleTimeout";
    public static final String APP_BLOCKING_TIMEOUT_KEY              = "blockingTimeout";
    public static final String APP_PREPARED_STATEMENT_CACHE_SIZE_KEY = "preparedStatementCacheSize";
    public static final String APP_ORACLE_CON_TYPE_KEY               = "oracleConType";
    public static final String APP_CON_PROP_KEY                      = "connectionProperties";
    public static final String PASSWD_ENC_PASSWD_KEY                 = "encPasswd";
    public static final String PASSWD_ENC_KEY_KEY                    = "encKey";
    /**
     * 写，次数限制
     */
    public static final String APP_WRITE_RESTRICT_TIMES              = "writeRestrictTimes";
    /**
     * 读，次数限制
     */
    public static final String APP_READ_RESTRICT_TIMES               = "readRestrictTimes";
    /**
     * thread count 次数限制
     */
    public static final String APP_THREAD_COUNT_RESTRICT             = "threadCountRestrict";

    public static final String APP_TIME_SLICE_IN_MILLS               = "timeSliceInMillis";

    public static TAtomDsConfDO parserTAtomDsConfDO(String globaConfStr, String appConfStr) {
        TAtomDsConfDO pasObj = new TAtomDsConfDO();
        if (StringUtil.isNotBlank(globaConfStr)) {
            Properties globaProp = TAtomConfParser.parserConfStr2Properties(globaConfStr);
            if (!globaProp.isEmpty()) {
                String ip = StringUtil.trim(globaProp.getProperty(TAtomConfParser.GLOBA_IP_KEY));
                if (StringUtil.isNotBlank(ip)) {
                    pasObj.setIp(ip);
                }
                String port = StringUtil.trim(globaProp.getProperty(TAtomConfParser.GLOBA_PORT_KEY));
                if (StringUtil.isNotBlank(port)) {
                    pasObj.setPort(port);
                }
                String dbName = StringUtil.trim(globaProp.getProperty(TAtomConfParser.GLOBA_DB_NAME_KEY));
                if (StringUtil.isNotBlank(dbName)) {
                    pasObj.setDbName(dbName);
                }
                String dbType = StringUtil.trim(globaProp.getProperty(TAtomConfParser.GLOBA_DB_TYPE_KEY));
                if (StringUtil.isNotBlank(dbType)) {
                    pasObj.setDbType(dbType);
                }
                String dbStatus = StringUtil.trim(globaProp.getProperty(TAtomConfParser.GLOBA_DB_STATUS_KEY));
                if (StringUtil.isNotBlank(dbStatus)) {
                    pasObj.setDbStatus(dbStatus);
                }
            }
        }
        if (StringUtil.isNotBlank(appConfStr)) {
            Properties appProp = TAtomConfParser.parserConfStr2Properties(appConfStr);
            if (!appProp.isEmpty()) {
                String userName = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_USER_NAME_KEY));
                if (StringUtil.isNotBlank(userName)) {
                    pasObj.setUserName(userName);
                }
                String oracleConType = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_ORACLE_CON_TYPE_KEY));
                if (StringUtil.isNotBlank(oracleConType)) {
                    pasObj.setOracleConType(oracleConType);
                }
                String minPoolSize = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_MIN_POOL_SIZE_KEY));
                if (StringUtil.isNotBlank(minPoolSize) && StringUtil.isNumeric(minPoolSize)) {
                    pasObj.setMinPoolSize(Integer.valueOf(minPoolSize));
                }
                String maxPoolSize = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_MAX_POOL_SIZE_KEY));
                if (StringUtil.isNotBlank(maxPoolSize) && StringUtil.isNumeric(maxPoolSize)) {
                    pasObj.setMaxPoolSize(Integer.valueOf(maxPoolSize));
                }
                String idleTimeout = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_IDLE_TIMEOUT_KEY));
                if (StringUtil.isNotBlank(idleTimeout) && StringUtil.isNumeric(idleTimeout)) {
                    pasObj.setIdleTimeout(Long.valueOf(idleTimeout));
                }
                String blockingTimeout = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_BLOCKING_TIMEOUT_KEY));
                if (StringUtil.isNotBlank(blockingTimeout) && StringUtil.isNumeric(blockingTimeout)) {
                    pasObj.setBlockingTimeout(Integer.valueOf(blockingTimeout));
                }
                String preparedStatementCacheSize = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_PREPARED_STATEMENT_CACHE_SIZE_KEY));
                if (StringUtil.isNotBlank(preparedStatementCacheSize)
                    && StringUtil.isNumeric(preparedStatementCacheSize)) {
                    pasObj.setPreparedStatementCacheSize(Integer.valueOf(preparedStatementCacheSize));
                }

                String writeRestrictTimes = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_WRITE_RESTRICT_TIMES));
                if (StringUtil.isNotBlank(writeRestrictTimes) && StringUtil.isNumeric(writeRestrictTimes)) {
                    pasObj.setWriteRestrictTimes(Integer.valueOf(writeRestrictTimes));
                }

                String readRestrictTimes = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_READ_RESTRICT_TIMES));
                if (StringUtil.isNotBlank(readRestrictTimes) && StringUtil.isNumeric(readRestrictTimes)) {
                    pasObj.setReadRestrictTimes(Integer.valueOf(readRestrictTimes));
                }
                String threadCountRestrict = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_THREAD_COUNT_RESTRICT));
                if (StringUtil.isNotBlank(threadCountRestrict) && StringUtil.isNumeric(threadCountRestrict)) {
                    pasObj.setThreadCountRestrict(Integer.valueOf(threadCountRestrict));
                }
                String timeSliceInMillis = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_TIME_SLICE_IN_MILLS));
                if (StringUtil.isNotBlank(timeSliceInMillis) && StringUtil.isNumeric(timeSliceInMillis)) {
                    pasObj.setTimeSliceInMillis(Integer.valueOf(timeSliceInMillis));
                }

                String conPropStr = StringUtil.trim(appProp.getProperty(TAtomConfParser.APP_CON_PROP_KEY));
                Map<String, String> connectionProperties = parserConPropStr2Map(conPropStr);
                if (null != connectionProperties && !connectionProperties.isEmpty()) {
                    pasObj.setConnectionProperties(connectionProperties);
                }
            }
        }
        return pasObj;
    }

    public static Map<String, String> parserConPropStr2Map(String conPropStr) {
        Map<String, String> connectionProperties = null;
        if (StringUtil.isNotBlank(conPropStr)) {
            String[] keyValues = StringUtil.split(conPropStr, ";");
            if (null != keyValues && keyValues.length > 0) {
                connectionProperties = new HashMap<String, String>(keyValues.length);
                for (String keyValue : keyValues) {
                    String key = StringUtil.substringBefore(keyValue, "=");
                    String value = StringUtil.substringAfter(keyValue, "=");
                    if (StringUtil.isNotBlank(key) && StringUtil.isNotBlank(value)) {
                        connectionProperties.put(key, value);
                    }
                }
            }
        }
        return connectionProperties;
    }

    public static String parserPasswd(String passwdStr) {
        String passwd = null;
        Properties passwdProp = TAtomConfParser.parserConfStr2Properties(passwdStr);
        String encPasswd = passwdProp.getProperty(TAtomConfParser.PASSWD_ENC_PASSWD_KEY);
        if (StringUtil.isNotBlank(encPasswd)) {
            String encKey = passwdProp.getProperty(TAtomConfParser.PASSWD_ENC_KEY_KEY);
            try {
                passwd = SecureIdentityLoginModule.decode(encKey, encPasswd);
            } catch (Exception e) {
                logger.error("[parserPasswd Error] decode dbPasswdError!may jdk version error!", e);
            }
        }
        return passwd;
    }

    private static Properties parserConfStr2Properties(String data) {
        Properties prop = new Properties();
        if (StringUtil.isNotBlank(data)) {
            ByteArrayInputStream byteArrayInputStream = null;
            try {
                byteArrayInputStream = new ByteArrayInputStream((data).getBytes());
                prop.load(byteArrayInputStream);
            } catch (IOException e) {
                logger.error("parserConfStr2Properties Error", e);
            } finally {
                byteArrayInputStream.close();
            }
        }
        return prop;
    }
}
