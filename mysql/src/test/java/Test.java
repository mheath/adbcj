import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.mysql.MysqlConnectionManagerFactory;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Class.forName(MysqlConnectionManagerFactory.class.getName());
		
		ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/test", "mheath", "cockroach", executorService);
		DbFuture<Connection> connectFuture = connectionManager.connect();
		connectFuture.addListener(new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> listener) throws Exception {
				System.out.println("In connect callback 1.");
			}
		});
		Connection connection = connectFuture.get();
		System.out.println("Got connection");
		connectFuture.addListener(new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> listener) throws Exception {
				System.out.println("In connect callback 2.");
			}
		});
		connection.executeQuery("SELECT * FROM test").addListener(new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> listener) throws Exception {
				System.out.println("Result set count: " + listener.get().getFields().size());
			}
		}).get();
		System.out.println("Got result set");
		DbSessionFuture<Void> closeFuture = connection.close(true);
		closeFuture.get();
		
		System.out.println("Closed");
		connectionManager.close(true);
		Thread.sleep(500);
		
		connectionManager.connect().addListener(new DbListener<Connection>() {
			public void onCompletion(DbFuture<Connection> listener)
					throws Exception {
				System.out.println("Made second connection");
			}
		}).get().close(true).get();
		
		System.out.println("Done");
		executorService.shutdown();
	}

}
