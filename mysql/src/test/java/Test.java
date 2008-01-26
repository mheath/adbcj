import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSessionFuture;
import org.adbcj.ResultSet;
import org.adbcj.mysql.Adbcj;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Adbcj.init();
		
		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/test", "adbcjtck", "adbcjtck");
//		Connection connection = connectionManager.connect().addListener(new DbListener<Connection>() {
//			public void onCompletion(DbFuture<Connection> listener) throws Exception {
//				System.out.println("In connect callback 1.");
//			}
//		}).get();
//		System.out.println("Got connection");
//		connection.executeQuery("SELECT * FROM test").addListener(new DbListener<ResultSet>() {
//			public void onCompletion(DbFuture<ResultSet> listener) throws Exception {
//				System.out.println("Result set count: " + listener.get().getFields().size());
//			}
//		}).get();
//		System.out.println("Got result set");
//		DbSessionFuture<Void> closeFuture = connection.close(true);
//		closeFuture.get();
//		
//		System.out.println("Closed");
//		//connectionManager.close(true);
//		Thread.sleep(500);
//		
//		connectionManager.connect().addListener(new DbListener<Connection>() {
//			public void onCompletion(DbFuture<Connection> listener)
//					throws Exception {
//				System.out.println("Made second connection");
//			}
//		}).get().close(true).get();
//
		System.out.println(connectionManager.connect().cancel(true));
		
		connectionManager.close(true);
		System.out.println("Done");
	}

}
