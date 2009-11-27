package org.adbcj.perf;

import static org.adbcj.perf.Configuration.MYSQL_ADBCJ;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NETTY_NO_PIPELINING;
import static org.adbcj.perf.Configuration.MYSQL_ADBCJ_NO_PIPELINING;
import static org.adbcj.perf.Configuration.MYSQL_JDBC;
import static org.adbcj.perf.Configuration.MYSQL_JDBC_BATCH;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NETTY_NO_PIPELINING;
import static org.adbcj.perf.Configuration.POSTGRESQL_ADBCJ_NO_PIPELINING;
import static org.adbcj.perf.Configuration.POSTGRESQL_JDBC;
import static org.adbcj.perf.Configuration.POSTGRESQL_JDBC_BATCH;

import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class Benchmark {

	public static void main(String[] args) throws Exception {
		final String host;
		if (args.length > 0) {
			host = args[0];
		} else {
			//host = "localhost";
			//host = "10.108.37.24";  // rldb
			host = "192.168.0.15";
		}
		System.out.println("Database Host: " + host);
		System.out.println(System.getProperty("java.class.path"));

		// Load JDBC database drivers
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");

		// --- Create groups --------------------------------------------------

		// Connect
		List<Group> groups = new ArrayList<Group>();
		Group connect = new Group("Connect");
//		groups.add(connect);
		final int count = 200;
		connect.addExperiment(new JDBCConnectExperiment(MYSQL_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(MYSQL_ADBCJ_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(MYSQL_ADBCJ, host, count));
		//connect.addExperiment(new AdbcjConnectExperiment(MYSQL_ADBCJ_NETTY, host, count));
		connect.addExperiment(new JDBCConnectExperiment(POSTGRESQL_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(POSTGRESQL_ADBCJ_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(POSTGRESQL_ADBCJ, host, count));

		Group close = new Group("Close");
//		groups.add(finalizeClose);
		close.addExperiment(new JDBCCloseExperiment(MYSQL_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(MYSQL_ADBCJ_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(MYSQL_ADBCJ, host, count));
		//finalizeClose.addExperiment(new AdbcjCloseExperiment(MYSQL_ADBCJ_NETTY, host, count));
		close.addExperiment(new JDBCCloseExperiment(POSTGRESQL_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(POSTGRESQL_ADBCJ_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(POSTGRESQL_ADBCJ, host, count));


		final String tinyQuery = "SELECT 1";
		Group selectsTiny = new Group("Selects Tiny");
		groups.add(selectsTiny);
		selectsTiny.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, tinyQuery, count));
		selectsTiny.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY_NO_PIPELINING, host, tinyQuery, count));

		final String smallQuery = "SELECT * FROM simple_values";
		Group selectsSmall = new Group("Selects Small");
		groups.add(selectsSmall);
		selectsSmall.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, smallQuery, count));
		selectsSmall.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, smallQuery, count));

		final String largeQuery = "SELECT * FROM large";
		Group selectsLarge = new Group("Selects Large");
		groups.add(selectsLarge);
		selectsLarge.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, largeQuery, count));
		selectsLarge.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NETTY, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, largeQuery, count));

		Group updates = new Group("Updates");
//		groups.add(updates);
		updates.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC_BATCH, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NETTY,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_JDBC,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NO_PIPELINING,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC_BATCH, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_JDBC,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING,  host, "UPDATE updates SET id=%d", count));

		Group inserts = new Group("Inserts");
//		groups.add(inserts);
		inserts.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC_BATCH, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NETTY,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_JDBC,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NO_PIPELINING,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC_BATCH, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_JDBC,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING,  host, "INSERT INTO updates (id) VALUES (%d)", count));

		// === Multiplexing benchmarks ===

		final int MAX_CONNECTIONS = 50;
		final int multiplexCount = MAX_CONNECTIONS * 4;

		// Select tiny
//		for (int i = 1; i <= MAX_CONNECTIONS; i++) {
//			Group mxSelectTiny = new Group("Multiplex Select Tiny " + i);
//			groups.add(mxSelectTiny);
//			mxSelectTiny.addExperiment(new Multipen to lexedAdbcjQueryExperiment(MYSQL_ADBCJ, host, tinyQuery, multiplexCount, i));
//			//mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, tinyQuery, multiplexCount, i));
//			mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, tinyQuery, multiplexCount, i));
//			mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, tinyQuery, multiplexCount, i));
//			//mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, tinyQuery, multiplexCount, i));
//			mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, tinyQuery, multiplexCount, i));
//			mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, tinyQuery, multiplexCount, i));
//			mxSelectTiny.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, tinyQuery, multiplexCount, i));
//		}

		// Select small
//		for (int i = 1; i <= MAX_CONNECTIONS; i++) {
//			Group mxSelectSmall = new Group("Multiplex Select Small " + i);
//			groups.add(mxSelectSmall);
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ, host, smallQuery, multiplexCount, i));
//			//mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, smallQuery, multiplexCount, i));
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, smallQuery, multiplexCount, i));
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, smallQuery, multiplexCount, i));
//			//mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, smallQuery, multiplexCount, i));
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, smallQuery, multiplexCount, i));
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, smallQuery, multiplexCount, i));
//			mxSelectSmall.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, smallQuery, multiplexCount, i));
//		}

		// Select large
//		for (int i = 1; i <= MAX_CONNECTIONS; i++) {
//			Group mxSelectLarge = new Group("Multiplex Select Large " + i);
//			groups.add(mxSelectLarge);
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ, host, largeQuery, multiplexCount, i));
//			//mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY, host, largeQuery, multiplexCount, i));
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, largeQuery, multiplexCount, i));
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, largeQuery, multiplexCount, i));
//			//mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(MYSQL_ADBCJ_NETTY_NO_PIPELINING, host, largeQuery, multiplexCount, i));
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, largeQuery, multiplexCount, i));
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, largeQuery, multiplexCount, i));
//			mxSelectLarge.addExperiment(new MultiplexedAdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, largeQuery, multiplexCount, i));
//		}
		runExperiments(groups);


	}

	public static void runExperiments(List<Group> groups) throws FileNotFoundException {
		// Run experiments
		List<Experiment> experiments = new ArrayList<Experiment>();
		for (Group group: groups) {
			experiments.addAll(group.getExperiments());
		}
		System.out.println("Warm up JIT");
		// Run each experiment twice to warm up JIT
		runAll(experiments);
		runAll(experiments);

		// Reset experiment stats
		for (Experiment experiment : experiments) {
			experiment.reset();
		}


		// Run all experiments
		System.out.println("Run all");
		final int runCount = 300;
		//final int runCount = 10;
		for (int i = 0; i < runCount; i++) {
			Collections.shuffle(experiments);
			runAll(experiments);
			System.out.printf("========= Run %d out of %d =================================\n", i + 1, runCount);
		}

		// Ouput results
		System.out.println("Output results");
		for (Group group: groups) {
			final PrintStream out = new PrintStream(group.getName() + ".txt");
			group.dump(out, true);
		}
	}

	public static void runAll(List<Experiment> experiments) {
		for (Experiment experiment : experiments) {
			System.out.println("Running: " + experiment.getConfiguration() + " " + experiment.getGroup().getName());
			experiment.run();
		}
	}
}
