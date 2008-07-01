package org.adbcj.perf;

import static org.adbcj.perf.Configuration.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintStream;

/**
 *
 */
public class Benchmark {

	public static void main(String[] args) throws Exception {
		final String host;
		if (args.length > 0) {
			host = args[0];
		} else {
			host = "localhost";
		}
		System.out.println("Database Host: " + host);

		// Load database drivers
		org.adbcj.mysql.Adbcj.init();
		org.adbcj.postgresql.Adbcj.init();
		org.adbcj.jdbc.Adbcj.init();
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");

		// --- Create groups --------------------------------------------------

		// Connect
		List<Group> groups = new ArrayList<Group>();
		Group connect = new Group("Connect");
		groups.add(connect);
		final int count = 50;
		connect.addExperiment(new JDBCConnectExperiment(MYSQL_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(MYSQL_ADBCJ_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(MYSQL_ADBCJ, host, count));
		connect.addExperiment(new JDBCConnectExperiment(POSTGRESQL_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(POSTGRESQL_ADBCJ_JDBC, host, count));
		connect.addExperiment(new AdbcjConnectExperiment(POSTGRESQL_ADBCJ, host, count));

		Group close = new Group("Close");
		groups.add(close);
		close.addExperiment(new JDBCCloseExperiment(MYSQL_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(MYSQL_ADBCJ_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(MYSQL_ADBCJ, host, count));
		close.addExperiment(new JDBCCloseExperiment(POSTGRESQL_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(POSTGRESQL_ADBCJ_JDBC, host, count));
		close.addExperiment(new AdbcjCloseExperiment(POSTGRESQL_ADBCJ, host, count));


		final String tinyQuery = "SELECT 1";
		Group selectsTiny = new Group("Selects Tiny");
		groups.add(selectsTiny);
		selectsTiny.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, tinyQuery, count));
		selectsTiny.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, tinyQuery, count));
		selectsTiny.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, tinyQuery, count));

		final String smallQuery = "SELECT * FROM simple_values";
		Group selectsSmall = new Group("Selects Small");
		groups.add(selectsSmall);
		selectsSmall.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, smallQuery, count));
		selectsSmall.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, smallQuery, count));
		selectsSmall.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, smallQuery, count));

		final String largeQuery = "SELECT * FROM large";
		Group selectsLarge = new Group("Selects Large");
		groups.add(selectsLarge);
		selectsLarge.addExperiment(new JDBCQueryExperiment(MYSQL_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(MYSQL_ADBCJ_NO_PIPELINING, host, largeQuery, count));
		selectsLarge.addExperiment(new JDBCQueryExperiment(POSTGRESQL_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_JDBC, host, largeQuery, count));
		selectsLarge.addExperiment(new AdbcjQueryExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING, host, largeQuery, count));

		Group updates = new Group("Updates");
		groups.add(updates);
		updates.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC_BATCH, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_JDBC,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NO_PIPELINING,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC_BATCH, host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_JDBC,  host, "UPDATE updates SET id=%d", count));
		updates.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING,  host, "UPDATE updates SET id=%d", count));

		Group inserts = new Group("Inserts");
		groups.add(inserts);
		inserts.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(MYSQL_JDBC_BATCH, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_JDBC,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(MYSQL_ADBCJ_NO_PIPELINING,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new JdbcUpdateExperiment(POSTGRESQL_JDBC_BATCH, host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_JDBC,  host, "INSERT INTO updates (id) VALUES (%d)", count));
		inserts.addExperiment(new AdbcjUpdateExperiment(POSTGRESQL_ADBCJ_NO_PIPELINING,  host, "INSERT INTO updates (id) VALUES (%d)", count));

		// Run experiments
		List<Experiment> experiments = new ArrayList<Experiment>();
		for (Group group: groups) {
			experiments.addAll(group.getExperiments());
		}
		// Run each experiment twice to warm up JIT
		runAll(experiments);
		runAll(experiments);

		// Reset experiment stats
		for (Experiment experiment : experiments) {
			experiment.reset();
		}


		// Run all experiments
		for (int i = 0; i < 25; i++) {
			Collections.shuffle(experiments);
			runAll(experiments);
		}

		// Ouput results
		for (Group group: groups) {
			final PrintStream out = new PrintStream(group.getName() + ".txt");
			group.dump(out, true);
		}
	}

	private static void runAll(List<Experiment> experiments) {
		for (Experiment experiment : experiments) {
			System.out.println("Running: " + experiment.getConfiguration());
			experiment.run();
		}
	}
}
