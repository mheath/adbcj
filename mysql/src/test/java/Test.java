import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerFactory;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.mysql.MysqlConnectionManagerProducer;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Class.forName(MysqlConnectionManagerProducer.class.getName());
		
		ConnectionManager connectionManager = ConnectionManagerFactory.createConnectionManager("adbcj:mysql://localhost/test", "mheath", "cockroach");
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
		DbSessionFuture<Void> closeFuture = connection.close(true);
		closeFuture.get();
		System.out.println("Closed");
	}

}
