package org.adbcj.perf;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbFuture;
import org.adbcj.DbSession;
import org.adbcj.ResultSet;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * If you have lots of connections to a single host, how long does it take to run a bunch of queries.
 *
 * @author Mike Heath
 */
public class VerticalBenchmark {

	private static final String USER = "adbcjtck";
	private static final String PASSWORD = "adbcjtck";

	public static void main(String[] args) throws Exception {
		// Load JDBC database drivers
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");

		if (args.length != 2) {
			throw new IllegalStateException("You must pass 2 arguments: count url");
		}

		final int count = Math.max(1, Integer.parseInt(args[0]));
		final String url = args[1];

		System.out.println("Creating connection manager to " + url);
		final ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager(url, USER, PASSWORD);

		System.err.println("Creating connections");

		DbSession[] sessions = new DbSession[count];
		for (int i = 0; i < count; i++) {
			sessions[i] = connectionManager.connect().getUninterruptably();
		}

		System.err.println("Warming up the JIT");
		for (int i = 0; i < 1000; i++) {
			run(sessions);
		}
		System.err.println("Running");

		final DescriptiveStatistics timings = new DescriptiveStatistics();
		for (int i = 0; i < 50000; i++) {
			timings.addValue(run(sessions));
		}
		OutputStream out = new FileOutputStream("verticalresults.txt", true);
		PrintWriter writer = new PrintWriter(out);
		writer.printf("%s %d %f %f\n", url, count, timings.getMean(), timings.getStandardDeviation());
		writer.flush();
		System.exit(0);

	}

	private static long run(DbSession[] sessions) {
		final long start = System.nanoTime();
		List<DbFuture<ResultSet>> futures = new ArrayList<DbFuture<ResultSet>>(sessions.length);
		for (DbSession session : sessions) {
			futures.add(session.executeQuery("SELECT * FROM simple_values"));
		}
		for (DbFuture<ResultSet> future : futures) {
			future.getUninterruptably();
		}
		return System.nanoTime() - start;
	}
	
}
