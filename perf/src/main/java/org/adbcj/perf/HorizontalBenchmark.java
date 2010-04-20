package org.adbcj.perf;

import org.adbcj.ConnectionManager;
import org.adbcj.DbFuture;
import org.adbcj.DbSession;
import org.adbcj.ResultSet;
import org.adbcj.jdbc.JdbcConnectionManager;
import org.adbcj.mysql.netty.MysqlConnectionManager;
import org.adbcj.postgresql.netty.NettyConnectionManager;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * If you have lots of hosts, how long does it take to query each.
 *
 * @author Mike Heath
 */
public class HorizontalBenchmark {

	private static enum Database {
		MYSQL,
		MYSQL_JDBC,
		POSTGRES,
		POSTGRES_JDBC
	}

	private static final String USER = "adbcjtck";
	private static final String PASSWORD = "adbcjtck";

	public static void main(String[] args) throws Exception {
		// Load JDBC database drivers
		Class.forName("com.mysql.jdbc.Driver");
		Class.forName("org.postgresql.Driver");

		if (args.length < 3) {
			throw new IllegalStateException("You must pass at least 3 arguments: database (MYSQL, MYSQL_JDBC, POSTGRES, POSTGRES_JDBC) count host(s)");
		}

		Database database = Database.valueOf(args[0]);
		int count = Integer.parseInt(args[1]);
		String[] hosts = Arrays.copyOfRange(args, 2, args.length);

		ExecutorService executor = Executors.newCachedThreadPool();
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(executor, executor);

		System.out.println("Creating connection managers");
		final ConnectionManager[] connectionManagers = new ConnectionManager[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			switch (database) {
				case MYSQL:
					connectionManagers[i] = new MysqlConnectionManager(hosts[i], 3306, USER, PASSWORD, USER, null, channelFactory);
					break;
				case MYSQL_JDBC:
					connectionManagers[i] = new JdbcConnectionManager("jdbc:mysql://" + hosts[i] + "/adbcjtck", USER, PASSWORD, executor, null);
					break;
				case POSTGRES:
					connectionManagers[i] = new NettyConnectionManager(hosts[i], 5432, USER, PASSWORD, USER, null, channelFactory);
					break;
				case POSTGRES_JDBC:
					connectionManagers[i] = new JdbcConnectionManager("jdbc:postgresql://" + hosts[i] + "/adbcjtck", USER, PASSWORD, executor, null);
					break;
			}
		}

		System.err.println("Creating connections");

		DbSession[] sessions = new DbSession[connectionManagers.length];
		for (int i = 0; i < connectionManagers.length; i++) {
			sessions[i] = connectionManagers[i].connect().getUninterruptably();
		}

		System.err.println("Warming up the JIT");
		for (int i = 0; i < 100; i++) {
			run(sessions);
		}

		final DescriptiveStatistics timings = new DescriptiveStatistics();
		for (int i = 0; i < count; i++) {
			timings.addValue(run(sessions));
		}
		OutputStream out = new FileOutputStream("horizontalresults.txt", true);
		PrintWriter writer = new PrintWriter(out);
		writer.printf("%s %d %f %f %d\n", database, hosts.length, timings.getMean(), timings.getStandardDeviation(), count);
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
