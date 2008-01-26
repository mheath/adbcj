package org.adbcj.mysql;

public class Adbcj {

	static {
		init();
	}
	
	public static void init() {
		MysqlConnectionManagerFactory.register();
	}
}
