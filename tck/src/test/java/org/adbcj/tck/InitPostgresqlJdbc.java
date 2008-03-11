package org.adbcj.tck;

import org.adbcj.jdbc.Adbcj;
import org.testng.annotations.BeforeSuite;

public class InitPostgresqlJdbc {
	@BeforeSuite
	public void registerDriver() throws ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Adbcj.init();
	}

}
