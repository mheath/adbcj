package org.adbcj.tck;

import org.testng.annotations.BeforeSuite;

public class InitMysqlJdbc {
	@BeforeSuite
	public void registerDriver() throws ClassNotFoundException {
		// Register the MySQL JDBC driver
		Class.forName("com.mysql.jdbc.Driver");
	}

}
