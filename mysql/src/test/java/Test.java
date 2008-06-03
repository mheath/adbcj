import org.adbcj.*;
import org.adbcj.mysql.Adbcj;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoBuffer;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.io.IOException;

public class Test {
	private static DbListener<ResultSet> listener;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		final Logger logger = LoggerFactory.getLogger("org.adbcj.mysql.Test");

		Adbcj.init();

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");

		DbSessionPool pool = new DbSessionPool();
		pool.addConnectionManager(connectionManager, 50);
		Thread.sleep(500);
		DbSession session = pool.connect().get();

//		DbSession session = connectionManager.connect().get();

		for (int i = 0; i < 200; i++) {
			listener = new DbListener<ResultSet>() {
				public void onCompletion(DbFuture<ResultSet> future) throws Exception {
					ResultSet rs = future.get();
					logger.info("Connection id: {}", rs.get(0).get("id"));
				}
			};
			session.executeQuery("SELECT connection_id() as id").addListener(listener);
			session.executeQuery("SELECT uuid() as id").addListener(listener);
		}

		//connectionManager.close(true);
	}

}
