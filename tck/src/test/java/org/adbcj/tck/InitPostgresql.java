package org.adbcj.tck;

import org.testng.annotations.BeforeSuite;
import org.adbcj.postgresql.Adbcj;

/**
 *
 */
public class InitPostgresql {

	@BeforeSuite
	public void registerDriver() {
		Adbcj.init();
	}

}
