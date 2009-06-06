package org.adbcj.tck;

import org.testng.annotations.BeforeSuite;

public class InitPostgresqlJdbc {
	@BeforeSuite
	public void registerDriver() throws ClassNotFoundException {
		// Register Postgresql JDBC driver
		Class.forName("org.postgresql.Driver");
	}

}
