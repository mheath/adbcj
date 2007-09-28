package org.safehaus.adbcj.jdbc;

public class Adbcj {

	static {
		init();
	}

	public static void init() {
		JdbcConnectionManagerFactory.register();
	}
	
}
