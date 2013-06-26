package com.taobao.tdhs.config;


public interface DbPasswdManager {
	
	/**��ȡ��ݿ�����
	 * @return
	 */
	public String getPasswd();
	
	/**ע��Ӧ�����ü���
	 * 
	 * @param Listener
	 */
	public void registerPasswdConfListener(ConfigDataListener Listener);
	
	/**
	 * ֹͣDbPasswdManager
	 */
	public void stopDbPasswdManager();
}
