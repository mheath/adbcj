package org.adbcj.postgresql;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.postgresql.backend.PgBackendMessageDecoder;
import org.adbcj.postgresql.frontend.PgFrontendMessageEncoder;
import org.adbcj.postgresql.frontend.StartupMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgConnectionManager implements ConnectionManager {

	public static final String CODEC_NAME = PgConnectionManager.class.getName() + ".codec";

	private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory() {
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			return new PgBackendMessageDecoder();
		}
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			// TODO Determine if this should be a static instance
			return new PgFrontendMessageEncoder();
		}
	};

	private final Logger logger = LoggerFactory.getLogger(PgConnectionManager.class);
	
	private final NioSocketConnector socketConnector;

	private final InetSocketAddress address;
	private final String username;
	private final String password;
	private final String database;
	
	private DefaultDbFuture<Void> closeFuture = null;

	public PgConnectionManager(String host, int port, String username, String password, String database,
			ExecutorService executorService, Properties properties) {
		logger.debug("Creating new PostgresqlConnectionManager");
		
		if (executorService == null) {
	        executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    1L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());
		}
		
		socketConnector = new NioSocketConnector();

		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();

		filterChain.addLast(CODEC_NAME, new ProtocolCodecFilter(CODEC_FACTORY));
		
		socketConnector.setHandler(new PgIoHandler(this));

		address = new InetSocketAddress(host, port);
		
		this.username = username;
		this.password = password;
		this.database = database;
	}

	public DbFuture<Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager closed");
		}
		final ConnectFuture connectFuture = socketConnector.connect(address);

		final DefaultDbFuture<Connection> dbConnectFuture = new DefaultDbFuture<Connection>() {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				logger.trace("Cancelling connect");
				connectFuture.cancel();
				if (connectFuture.isCanceled()) {
					logger.trace("Canceled connect");
					connectFuture.cancel();
					return true;
				}
				logger.trace("Did not cancel connect");
				return false;
			}
		};

		connectFuture.addListener(new IoFutureListener<ConnectFuture>() {
			public void operationComplete(ConnectFuture future) {
				logger.trace("connect future");

				IoSession session = future.getSession();
				
				// Put DbFuture<Connection> in session
				IoSessionUtil.setDbConnectFuture(session, dbConnectFuture);
				logger.trace("dbConnectFuture added to session");
				
				// Create Connection and add connection to future
				//DefaultDbFuture<Connection> dbConnectFuture = IoSessionUtil.getDbConnectFuture(session);
				PgConnection connection = new PgConnection(PgConnectionManager.this, session, dbConnectFuture);
				IoSessionUtil.setConnection(session, connection);
				dbConnectFuture.setValue(connection);

				// Send start message to backend
				Map<ConfigurationVariable, String> parameters = new HashMap<ConfigurationVariable, String>();
				parameters.put(ConfigurationVariable.CLIENT_ENCODING, "UNICODE");
				parameters.put(ConfigurationVariable.DATE_STYLE, "ISO");
				session.write(new StartupMessage(username, database, parameters));
			}
		});
		return dbConnectFuture;
	}

	public synchronized DbFuture<Void> close(boolean immediate) throws DbException {
		// TODO Put test in TCK to make sure all ConnectionManager connections get closed
		if (isClosed()) {
			return closeFuture;
		}
		closeFuture = new DefaultDbFuture<Void>();
		if (immediate) {
			socketConnector.dispose();
			closeFuture.setDone();
		} else {
			// TODO Implement PostgresqlConnectionManager.close(boolean)
			throw new IllegalStateException("Non immediate close not yet implemented");
		}
		return closeFuture;
	}

	public synchronized boolean isClosed() {
		return closeFuture != null;
	}
	
	// ================================================================================================================
	//
	// Non-API methods
	//
	// ================================================================================================================
	
	public String getUsername() {
		return username;
	}
	
	public String getDatabase() {
		return database;
	}
	
	public String getPassword() {
		return password;
	}

}
