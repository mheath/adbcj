package org.adbcj.mysql.netty;

public class Adbcj {

	static {
		init();
	}

	public static void init() {
		MySqlConnectionManagerFactory.register();
	}

}
