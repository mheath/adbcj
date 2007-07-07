import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerFactory;
import edu.byu.cs.adbcj.DbFuture;
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
		Connection connection = connectFuture.get();
		System.out.println("Got connection");
		DbSessionFuture<Void> closeFuture = connection.close(true);
		closeFuture.get();
		System.out.println("Closed");
	}

}
