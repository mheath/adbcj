package org.adbcj.perf;

import static org.adbcj.perf.Configuration.MYSQL_JDBC;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NO_PIPELINING;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY_NO_PIPELINING;
import static org.adbcj.perf.Configuration.POSTGRESQL_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NO_PIPELINING;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintStream;

/**
 * @author Mike Heath
 */
public class MinaVsNettyBenchmark {
	public static void main(String[] args) throws Exception {
		final String host;
		if (args.length > 0) {
			host = args[0];
		} else {
			host = "localhost";
			//host = "10.108.37.24";  // rldb
			//host = "192.168.0.15";
		}
		System.out.println("Database Host: " + host);
		System.out.println(System.getProperty("java.class.path"));

		// Load JDBC database drivers
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");


		List<Group> groups = new ArrayList<Group>();
		final int count = 50;

		// --- Create groups --------------------------------------------------
		final String smallQuery = "SELECT * FROM simple_values";
		Group selectsSmall = new Group("Selects Small");
		groups.add(selectsSmall);
		selectsSmall.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, smallQuery, count));

		Benchmark.runExperiments(groups);
	}
}
