package org.adbcj.postgresql.mina;

import org.adbcj.postgresql.codec.AbstractConnectionManager;
import org.adbcj.postgresql.codec.AbstractConnection;
import org.adbcj.postgresql.mina.PgIoHandler;
import org.adbcj.postgresql.mina.IoSessionUtil;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.Connection;
import org.adbcj.DbFuture;
import org.adbcj.DbException;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.net.InetSocketAddress;

/**
 * @author Mike Heath
 */
public class MinaConnectionManager extends AbstractConnectionManager {
	public static final String CODEC_NAME = AbstractConnectionManager.class.getName() + ".codec";

	private final Logger logger = LoggerFactory.getLogger(AbstractConnectionManager.class);

	private final NioSocketConnector socketConnector;

	private final String username;
	private final String password;
	private DefaultDbFuture<Void> closeFuture = null;

	private volatile boolean pipeliningEnabled = true;

	private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory() {
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			// TODO Determine if this should be a static instance
			return null;//new BackendMessageDecoder();
		}
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			// TODO Determine if this should be a static instance
			return null; //new PgFrontendMessageEncoder();
		}
	};

	public MinaConnectionManager(String host, int port, String username, String password, String database,
			Properties properties) {
		super(database);
		logger.debug("Creating new Postgresql ConnectionManager");

		socketConnector = new NioSocketConnector();

		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();

		filterChain.addLast(CODEC_NAME, new ProtocolCodecFilter(CODEC_FACTORY));

		socketConnector.setHandler(new PgIoHandler(this));

		InetSocketAddress address = new InetSocketAddress(host, port);
		socketConnector.setDefaultRemoteAddress(address);

		this.username = username;
		this.password = password;
	}

	public DbFuture<Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager closed");
		}
		logger.debug("Starting connection");
		PgConnectFuture future = new PgConnectFuture();
		//socketConnector.connect(future);

		logger.debug("Started connection");

		return future;
	}

	class PgConnectFuture extends DefaultDbFuture<Connection> implements IoSessionInitializer<ConnectFuture> {

		private boolean cancelled = false;
		private boolean started = false;

		@Override
		public synchronized void initializeSession(IoSession session, ConnectFuture future) {
			if (cancelled) {
				session.close();
				return;
			}
			logger.debug("Creating AbstractConnection");

			MinaConnection connection = new MinaConnection(MinaConnectionManager.this, this, session);
			IoSessionUtil.setConnection(session, connection);
		}

		@Override
		protected synchronized boolean doCancel(boolean mayInterruptIfRunning) {
			if (started) {
				logger.debug("Can't cancel, connection already started");
				return false;
			}
			logger.debug("Cancelled connect");
			cancelled = true;
			return true;
		}

	}

	public synchronized DbFuture<Void> close(boolean immediate) throws DbException {
		// TODO Put test in TCK to make sure all ConnectionManager connections get closed
		if (isClosed()) {
			return closeFuture;
		}
		closeFuture = new DefaultDbFuture<Void>();
		if (immediate) {
			socketConnector.dispose();
			closeFuture.setResult(null);
		} else {
			// TODO Implement PostgresqlConnectionManager.close(boolean)
			throw new IllegalStateException("Non immediate close not yet implemented");
		}
		return closeFuture;
	}

	public synchronized boolean isClosed() {
		return closeFuture != null;
	}

	public boolean isPipeliningEnabled() {
		return pipeliningEnabled;
	}

	public void setPipeliningEnabled(boolean pipeliningEnabled) {
		this.pipeliningEnabled = pipeliningEnabled;
	}

	// ================================================================================================================
	//
	// Non-API methods
	//
	// ================================================================================================================

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return String.format("Postgresql Connection Manager (Db: '%s', User: '%s')", getDatabase(), username);
	}


}
