package org.adbcj.tck;

import org.testng.annotations.BeforeSuite;
import org.adbcj.mysql.netty.Adbcj;

/**
 *
 */
public class InitMysqlNetty {

	@BeforeSuite
	public void registerDriver() {
		Adbcj.init();
	}

}
