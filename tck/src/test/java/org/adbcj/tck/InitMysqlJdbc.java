package org.adbcj.tck;

import org.adbcj.jdbc.Adbcj;
import org.testng.annotations.BeforeSuite;

public class InitMysqlJdbc {
	@BeforeSuite
	public void registerDriver() throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		Adbcj.init();
	}

}
