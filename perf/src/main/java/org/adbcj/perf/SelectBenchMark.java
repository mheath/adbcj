package org.adbcj.perf;

import org.adbcj.*;
import org.adbcj.mysql.Adbcj;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

/**
 * 
 */
public class SelectBenchMark {

	public void benchMarkSelect(DbSession session, String query, int count) throws InterruptedException {
		final long totalStart = System.nanoTime();
//		final Queue<Long> timings = new ConcurrentLinkedQueue<Long>();
		final CountDownLatch latch = new CountDownLatch(count);
		for (int i = 0; i < 1000; i++) {
			session.executeQuery(query).addListener(new DbListener<ResultSet>() {
				//private final long start = System.nanoTime();
				public void onCompletion(DbFuture<ResultSet> resultSetDbFuture) throws Exception {
//					timings.add(System.nanoTime() - start);
					try {
						resultSetDbFuture.get();
					} catch (DbException e) {
						e.printStackTrace();
					} finally {
						latch.countDown();
					}
				}
			});
		}
		latch.await();
		System.out.println(System.nanoTime() - totalStart);
//		for (Long timing : timings) {
//			System.out.println(timing);
//		}
	}

	public static void main(String[] args) throws InterruptedException {
		Adbcj.init();

		ConnectionManager mysqlConnectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		mysqlConnectionManager.setPipeliningEnabled(false);
		DbSession session = mysqlConnectionManager.connect().get();
		mysqlConnectionManager.setPipeliningEnabled(true);
		DbSession pipelinedSession = mysqlConnectionManager.connect().get();

		DbSessionPool sessionPool = new DbSessionPool();
		sessionPool.addConnectionManager(mysqlConnectionManager, 5);

		DbSession pooledSession = sessionPool.connect().get();

		SelectBenchMark benchMark = new SelectBenchMark();

		System.out.println("MySQL - Micro Warm Up");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small Warm Up");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT 1", 1000);
		System.out.println("MySQL - Small - No Pipe");
		benchMark.benchMarkSelect(session, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL - Micro - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT 1", 1000);
		System.out.println("MySQL - Small - Pipe");
		benchMark.benchMarkSelect(pipelinedSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL Pool - Micro - Pipe");
		benchMark.benchMarkSelect(pooledSession, "SELECT 1", 1000);
		System.out.println("MySQL Pool - Small - Pipe");
		benchMark.benchMarkSelect(pooledSession, "SELECT * FROM simple_values", 1000);

		System.out.println("MySQL Pool - Micro - Pipe");
		benchMark.benchMarkSelect(pooledSession, "SELECT 1", 1000);
		System.out.println("MySQL Pool - Small - Pipe");
		benchMark.benchMarkSelect(pooledSession, "SELECT * FROM simple_values", 1000);

		mysqlConnectionManager.close(true);
	}

}
