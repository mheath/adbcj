package com.taobao.tdhs.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.manager.DiamondManager;
import com.taobao.diamond.manager.ManagerListener;
import com.taobao.diamond.manager.impl.DefaultDiamondManager;

/**
 * @author shenxun
 * @author <a href="zylicfc@gmail.com">junyu</a>
 * @version 1.0
 * @since 1.6
 * @date 2011-1-11����11:22:29
 * @desc �־���������diamondʵ��
 */
public class DiamondConfigDataHandler implements ConfigDataHandler {
	private static final Log logger = LogFactory
			.getLog(DiamondConfigDataHandler.class);
	private DiamondManager diamondManager;
	private String dataId;
	private String mbeanId;

	public void init(final String dataId,
			final List<ConfigDataListener> configDataListenerList,
			final Map<String, Object> config) {
		mbeanId = dataId + System.currentTimeMillis();
		DiamondConfig.handleConfig(config);
		DefaultDiamondManager.Builder builder = new DefaultDiamondManager.Builder(
				dataId, new ManagerListener() {
					public void receiveConfigInfo(String data) {
						if (configDataListenerList != null) {
							for (ConfigDataListener configDataListener : configDataListenerList) {
								configDataListener.onDataRecieved(dataId, data);
							}
						}
					}

					public Executor getExecutor() {
						return (Executor) config.get("executor");
					}
				});
		String group = (String) config.get("group");
		if (null != group) {
			builder.setGroup(group);
		}
		this.diamondManager = builder.build();
		this.dataId = dataId;

	}

	public String getData(long timeout, String strategy) {
		String data = null;
		if (strategy != null
				&& strategy
						.equals(ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY)) {
			data = diamondManager.getAvailableConfigureInfomation(timeout);
		} else if (strategy != null
				&& strategy.equals(ConfigDataHandler.FIRST_SERVER_STRATEGY)) {
			data = diamondManager.getConfigureInfomation(timeout);
		}

		
		return data;
	}

	public void addListener(final ConfigDataListener configDataListener,
			final Executor executor) {
		if (configDataListener != null) {
			diamondManager.setManagerListener(new ManagerListener() {
				public void receiveConfigInfo(String data) {
					configDataListener.onDataRecieved(dataId, data);
				}

				public Executor getExecutor() {
					return executor;
				}
			});
		}
	}

	public void addListeners(
			final List<ConfigDataListener> configDataListenerList,
			final Executor executor) {
		if (configDataListenerList != null) {
			diamondManager.setManagerListener(new ManagerListener() {
				public void receiveConfigInfo(String data) {
					for (ConfigDataListener configDataListener : configDataListenerList) {
						try {
							configDataListener.onDataRecieved(dataId, data);
						} catch (Exception e) {
							logger.error("one of listener failed", e);
							continue;
						}
					}

				}

				public Executor getExecutor() {
					return executor;
				}
			});
		}
	}

	public void closeUnderManager() {
		diamondManager.close();
	}

	public String getDataId() {
		return dataId;
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
}
