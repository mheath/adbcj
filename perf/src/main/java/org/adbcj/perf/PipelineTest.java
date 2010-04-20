package org.adbcj.perf;

import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY_NO_PIPELINING;
import static org.adbcj.perf.Configuration.MYSQL_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY_NO_PIPELINING;
import static org.adbcj.perf.Configuration.POSTGRESQL_JDBC;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Heath
 */
public class PipelineTest {
	public static void main(String[] args) throws Exception {
		final String[] hosts;
		if (args.length > 0) {
			hosts = args;
		} else {
			hosts = new String[] {"localhost"};
			//host = "10.108.37.24";  // rldb
			//host = "192.168.0.15";
		}
		System.out.println("Database Hosts: " + hosts);
		System.out.println(System.getProperty("java.class.path"));

		// Load JDBC database drivers
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");

		List<Group> groups = new ArrayList<Group>();

		final String smallQuery = "SELECT * FROM simple_values";
		for (int i = 1; i <= 25; i++) {
			Group group = new Group("Pipeline Selects " + i);
			groups.add(group);
			group.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(Configuration.MYSQL_ADBCJ_JDBC, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, smallQuery, i, hosts));
			group.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(Configuration.POSTGRESQL_ADBCJ_JDBC, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, smallQuery, i, hosts));
			group.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY_NO_PIPELINING, smallQuery, i, hosts));
		}
		Benchmark.runExperiments(groups);
	}
}
