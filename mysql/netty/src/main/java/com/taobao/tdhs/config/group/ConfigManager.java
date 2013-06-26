package com.taobao.tdhs.config.group;

import java.util.ArrayList;
import java.util.List;

import org.adbcj.ConnectionManager;
import org.adbcj.mysql.netty.AtomMysqlConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.tdhs.config.ConfigDataHandler;
import com.taobao.tdhs.config.ConfigDataHandlerFactory;
import com.taobao.tdhs.config.ConfigDataListener;
import com.taobao.tdhs.config.DefaultConfigDataHandlerFactory;

/**
 * 一个ConfigManager对应一个TGroupDataSource，
 * 主要用于将根据Group的dataID取得的对应配置字符串信（比如db0:rwp1q1i0, db1:rwp0q0i1），
 * 转化为真正的Group层的配置体系结构：一个Group层挂着两个Atom db0 与 db1 ， 则我们使用一个 Map<String,
 * DataSourceWrapper> 来表示 其中的String 为每个Atom DS 的dbKey ，DataSourceWrapper
 * 为经过封装的TAtomDataSource
 * ---这里需要解释一下，为什么不直接使用AtomDataSource？因为每个AtomDataSource还有相应的权重和优先级信息 因此，需要***方法
 * 其中，配置的每一个Atom DataSource也只是用Atom
 * 的dbKey表示，因此，我们还需要根据此dbKey取得Atom的配置信息，并且将它封装成一个AtomDataSource对象。 因此需要***方法
 * 有了这个map能根据dbKey迅速的找到对应的Datasource也是不够的，我们的Group层应该是对应用透明的，
 * 因此，当我们的读写请求进来时，Group层应该能够根据配置的权重和优先级，自动的选择一个合适的DB上进行读写，
 * 所以，我们还需要将配置信息生成一个DBSelector来自动的完成根据权重、优先级选择合适的目标库 因此，需要***方法
 * 
 * @author yangzhu
 * @author linxuan refactor
 */
public class ConfigManager {

    private static final Log         logger               = LogFactory.getLog(ConfigManager.class);

    private final ConfigDataListener configReceiver;                                               // //动态接收Diamond推送过来的信息
    private ConfigDataHandlerFactory configFactory;
    private ConfigDataHandler        globalHandler;

    // add by junyu

    private String                   fullGroupKey;
    private String                   appName              = null;

    private int                      configReceiveTimeout = 20000;
    private List<ConnectionManager>  atomTdhsClient       = new ArrayList<ConnectionManager>();
    private WeightSelector           writeSelector        = new WeightSelector();
    private WeightSelector           readSelector         = new WeightSelector();

    public ConfigManager(){
        configReceiver = new ConfigReceiver();
    }

    /**
     * 从Diamond配置中心提取信息，构造TAtomDataSource、构造有优先级信息的读写DBSelector ---add by
     * mazhidan.pt
     */
    public void init() {
        // 警告: 不要在构造DefaultDiamondManager时就注册ManagerListener(比如:configReceiver)
        // 也就是说，不要这样用: new DefaultDiamondManager(dbGroupKey, configReceiver)，
        // 而是要设成null，等第一次取得信息并解析完成后再注册，这样可以不用同步，避免任何与并发相关的问题，
        // 因为有可能在第一次刚取回信息后，Diamond配置中心那边马上修改了记录，导致ManagerListener这个线程立刻收到信息，
        // 造成初始化线程和ManagerListener线程同时解析信息。
        configFactory = new DefaultConfigDataHandlerFactory();
        globalHandler = configFactory.getConfigDataHandler(fullGroupKey, null);

        String dsWeightCommaStr = globalHandler.getData(configReceiveTimeout,
            ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY);
        String[] dbArray = dsWeightCommaStr.split(","); // 逗号分隔：db0:rwp1q1i0,
        for (String db : dbArray) {
            String[] dbNameAndWeight = db.split(":");
            if (dbNameAndWeight.length != 2) {
                throw new IllegalArgumentException("weight can't find " + db);
            }
            String dbKey = dbNameAndWeight[0];
            String weightStr = dbNameAndWeight[1];
            Weight weight = new Weight(weightStr);
            int w = weight.w;
            AtomMysqlConnectionManager one = new AtomMysqlConnectionManager();
            try {
                one.setAppName(appName);
                one.setDbKey(dbKey);
                one.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int index = atomTdhsClient.size();
            atomTdhsClient.add(one);
            for (int i = 0; i < w; i++) {
                writeSelector.add(index);
            }

            int r = weight.r;
            for (int i = 0; i < r; i++) {
                readSelector.add(index);
            }
        }

    }

    public String getFullGroupKey() {
        return fullGroupKey;
    }

    public void setFullGroupKey(String fullGroupKey) {
        this.fullGroupKey = fullGroupKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<ConnectionManager> getAtomTdhsClient() {
        return atomTdhsClient;
    }

    public void setAtomTdhsClient(List<ConnectionManager> atomTdhsClient) {
        this.atomTdhsClient = atomTdhsClient;
    }

    public WeightSelector getWriteSelector() {
        return writeSelector;
    }

    public void setWriteSelector(WeightSelector writeSelector) {
        this.writeSelector = writeSelector;
    }

    public WeightSelector getReadSelector() {
        return readSelector;
    }

    public void setReadSelector(WeightSelector readSelector) {
        this.readSelector = readSelector;
    }

    private class ConfigReceiver implements ConfigDataListener {

        public void onDataRecieved(String dataId, String data) {
            try {
                logger.warn("group ds data received !dataId:" + dataId + " data:" + data);
                logger.warn("not allow");
            } catch (Throwable t) {
                logger.error("动态解析配置信息时出现错误:" + data, t);
            }
        }
    }
}
