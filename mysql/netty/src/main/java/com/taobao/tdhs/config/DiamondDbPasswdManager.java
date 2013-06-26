package com.taobao.tdhs.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ���������Diamondʵ��
 * 
 * @author qihao
 * 
 */
public class DiamondDbPasswdManager implements DbPasswdManager {
	private static Log logger = LogFactory.getLog(DiamondDbPasswdManager.class);
	private String passwdConfDataId;
	private ConfigDataHandlerFactory configFactory;
	private ConfigDataHandler passwdHandler;
	private volatile List<ConfigDataListener> passwdConfListener = new ArrayList<ConfigDataListener>();

	public void init() {
		configFactory = new DefaultConfigDataHandlerFactory();
		Map<String, String> config = new HashMap<String, String>();
		config.put("group", TAtomConstants.DEFAULT_DIAMOND_GROUP);
		passwdHandler = configFactory.getConfigDataHandlerWithListenerListCE(
				passwdConfDataId, passwdConfListener,
				Executors.newSingleThreadScheduledExecutor(), config);
	}

	public String getPasswd() {
		if (null != passwdHandler) {
			String passwdStr = passwdHandler.getData(
					TDDLConstant.DIAMOND_GET_DATA_TIMEOUT,
					ConfigDataHandler.FIRST_CACHE_THEN_SERVER_STRATEGY);
			if (passwdStr == null) {
				logger.error("[getDataError] remote password string is empty !");
				return null;
			}
			return TAtomConfParser.parserPasswd(passwdStr);
		}
		logger.error("[getDataError] passwdConfig not init !");
		return null;
	}

	public void registerPasswdConfListener(ConfigDataListener Listener) {
		passwdConfListener.add(Listener);
	}

	public void setPasswdConfDataId(String passwdConfDataId) {
		this.passwdConfDataId = passwdConfDataId;
	}

	public void stopDbPasswdManager() {
		if (null != this.passwdHandler) {
			this.passwdHandler.closeUnderManager();
		}
	}
}
