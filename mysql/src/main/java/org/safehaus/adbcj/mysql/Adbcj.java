package org.safehaus.adbcj.mysql;

public class Adbcj {

	static {
		init();
	}
	
	public static void init() {
		MysqlConnectionManagerFactory.register();
	}
}
