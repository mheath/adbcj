import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSession;
import org.adbcj.DbSessionPool;
import org.adbcj.ResultSet;
import org.adbcj.mysql.mina.Adbcj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		final Logger logger = LoggerFactory.getLogger("org.adbcj.mysql.Test");

		Adbcj.init();

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");

//		DbSessionPool pool = new DbSessionPool();
//		pool.addConnectionManager(connectionManager, 50);
//		Thread.sleep(500);
//		DbSession session = pool.connect().get();

		DbSession session = connectionManager.connect().get();

		DbListener<ResultSet> listener = new DbListener<ResultSet>() {
			public void onCompletion(DbFuture<ResultSet> future) throws Exception {
				ResultSet rs = future.get();
				logger.info("Connection id: {}", rs.get(0).get("id"));
			}
		};
		//for (int i = 0; i < 1000; i++) {
			session.executeQuery("SELECT connection_id() as id").addListener(listener);
		//}

		//connectionManager.close(true);
	}

}
