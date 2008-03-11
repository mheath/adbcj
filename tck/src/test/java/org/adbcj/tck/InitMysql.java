package org.adbcj.tck;

import org.testng.annotations.BeforeSuite;
import org.adbcj.mysql.Adbcj;

/**
 *
 */
public class InitMysql {

	@BeforeSuite
	public void registerDriver() {
		Adbcj.init();
	}

}
