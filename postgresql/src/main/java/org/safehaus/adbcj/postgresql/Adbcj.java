package org.safehaus.adbcj.postgresql;

public class Adbcj {

	static {
		init();
	}
	
	public static void init() {
		PgConnectionManagerFactory.register();
	}
	
}
