package edu.byu.cs.adbcj.mysql;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;

public class MysqlConnectionManager implements ConnectionManager {

	private final SocketConnector socketConnector;
	
	private final String host;
	private final int port;
	
	private final LoginCredentials credentials;
	
	public MysqlConnectionManager(String host, int port, String username, String password, String schema, Properties properties) {
		int processorCount = Runtime.getRuntime().availableProcessors() + 1;
		socketConnector = new SocketConnector(processorCount,
				new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>())); // TODO Make configurable
		//socketConnector.setWorkerTimeout(5); // TODO: Make configurable
		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();
		
		DemuxingProtocolCodecFactory codecFactory = new DemuxingProtocolCodecFactory();
		codecFactory.register(new MysqlMessageDecoder());
		codecFactory.register(new LoginRequestEncoder());
		codecFactory.register(new CommandRequestEncoder());

		filterChain.addLast("codec", new ProtocolCodecFilter(codecFactory)); // TODO Make filter name a constant
		
		socketConnector.setHandler(new MysqlProtocolHandler());
		
		this.host = host;
		this.port = port;
		
		this.credentials = new LoginCredentials(username, password, schema);
	}
	
	public void close() throws DbException {
		throw new DbException("Not implemented yet"); // TODO Implement
	}

	public DbFuture<Connection> connect() {
		SocketAddress address = new InetSocketAddress(host, port);
		ConnectFuture connectFuture = socketConnector.connect(address);
		
		final AbstractDbFutureBase<Connection> dbConnectFuture = new AbstractDbFutureBase<Connection>() {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				return false;
			}
		};
		
		connectFuture.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				MysqlConnection connection = new MysqlConnection(MysqlConnectionManager.this, future.getSession(), credentials); 
				dbConnectFuture.setValue(connection);
				connection.setCurrentFuture(dbConnectFuture);
				
				IoSessionUtil.setMysqlConnection(future.getSession(), connection);
			}
		});
		
		return dbConnectFuture;
	}

}
