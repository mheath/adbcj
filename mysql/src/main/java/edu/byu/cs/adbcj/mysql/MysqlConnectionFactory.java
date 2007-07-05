package edu.byu.cs.adbcj.mysql;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;
import org.apache.mina.transport.socket.nio.SocketConnector;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionFactory;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.support.BaseDbFuture;

public class MysqlConnectionFactory implements ConnectionFactory {

	public static final String PROTOCOL = "mysql";
	public static final int DEFAULT_PORT = 3306;
	
	static {
		ConnectionManager.registerConnectionFactory(PROTOCOL, new MysqlConnectionFactory());
	}
	
	private static SocketConnector socketConnector;
	
	private MysqlConnectionFactory() {
	}
	
	public DbFuture<Connection> connect(String url, String username, String password, Properties properties) throws DbException {
		String host;
		int port;
		String database;
		/*
		 * Parse URL
		 */
		try {
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			host = uri.getHost();
			port = uri.getPort();
			if (port < 0) {
				port = DEFAULT_PORT;
			}
			database = uri.getPath().substring(1);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
		
		SocketConnector connector = getSocketConnector();
		SocketAddress address = new InetSocketAddress(host, port);
		ConnectFuture connectFuture = connector.connect(address);
		
		final LoginCredentials credentials = new LoginCredentials(username, password, database);
		connectFuture.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				IoSessionUtil.setSessionAttribute(future.getSession(), SessionId.CREDENTIALS, credentials);
			}
		});
		
		final DbFuture<Connection> dbConnectFuture = new BaseDbFuture<Connection>() {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				return false;
			}
		};
		
		return dbConnectFuture;
	}
	
	private static synchronized SocketConnector getSocketConnector() {
		if (socketConnector == null) {
			int processorCount = Runtime.getRuntime().availableProcessors() + 1;
			socketConnector = new SocketConnector(processorCount,
					new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>()));
			socketConnector.setWorkerTimeout(5);
			DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();
			
			DemuxingProtocolCodecFactory codecFactory = new DemuxingProtocolCodecFactory();
			codecFactory.register(new MysqlMessageDecoder());
			codecFactory.register(new LoginRequestEncoder());

			filterChain.addLast("codec", new ProtocolCodecFilter(codecFactory)); // TODO Make filter name constant
			
			socketConnector.setHandler(new MysqlProtocolHandler());
		}
		return socketConnector;
	}

}
