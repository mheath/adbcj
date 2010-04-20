package org.adbcj.perf;

import java.util.ArrayList;
import java.util.List;

import static org.adbcj.perf.Configuration.MYSQL_ADBCJ;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.MYSQL_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.POSTGRESQL_JDBC;

/**
 * @author Mike Heath
 */
public class MinaVsNettyBenchmark {
	public static void main(String[] args) throws Exception {
		final String[] hosts;
		if (args.length > 0) {
			hosts = args;
		} else {
			hosts = new String[] { "localhost" };
			//host = "10.108.37.24";  // rldb
			//host = "192.168.0.15";
		}
		System.out.println("Database Hosts: " + hosts);
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
		selectsSmall.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, smallQuery, count, hosts));
		selectsSmall.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, smallQuery, count, hosts));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, smallQuery, count, hosts));

		Benchmark.runExperiments(groups);
	}
}
