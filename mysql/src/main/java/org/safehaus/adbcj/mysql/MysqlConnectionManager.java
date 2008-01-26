/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.safehaus.adbcj.mysql;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionInitializer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlConnectionManager implements ConnectionManager {
	
	public static final String CODEC_NAME = MysqlConnectionManager.class.getName() + ".codec";

	private final Logger logger = LoggerFactory.getLogger(MysqlConnectionManager.class);

	private final NioSocketConnector socketConnector;
	
	private final LoginCredentials credentials;
	
	private final AtomicInteger id = new AtomicInteger();
	private final Set<MysqlConnection> connections = new HashSet<MysqlConnection>();
	
	private DbFuture<Void> closeFuture = null;
	
	private static final ProtocolEncoder ENCODER = new MysqlMessageEncoder();
	private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory() {
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			return new MysqlMessageDecoder(session);
		}
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			return ENCODER;
		}
	};
	
	public MysqlConnectionManager(String host, int port, String username, String password, String schema, Properties properties) {
		socketConnector = new NioSocketConnector();
		//socketConnector.setWorkerTimeout(5); // TODO Make MINA worker timeout configurable in MysqlConnectionManager
		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();
		
		filterChain.addLast(CODEC_NAME, new ProtocolCodecFilter(CODEC_FACTORY));
		
		socketConnector.setHandler(new MysqlIoHandler(this));
		socketConnector.setDefaultRemoteAddress(new InetSocketAddress(host, port));

		this.credentials = new LoginCredentials(username, password, schema);
	}
	
	public synchronized DbFuture<Void> close(boolean immediate) throws DbException {
		if (isClosed()) {
			return closeFuture;
		}
		// TODO: Close all open connections
		if (immediate) {
			socketConnector.dispose();
			DefaultDbFuture<Void> future = new DefaultDbFuture<Void>();
			future.setDone();
			closeFuture = future;
			return closeFuture;
		} else {
			// TODO In MysqlConnectionManager.close() implement deferred close
			throw new IllegalStateException("Deferred close not yet implemented");
		}
	}

	public synchronized boolean isClosed() {
		return closeFuture != null;
	}
	
	public DbFuture<Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager closed");
		}
		logger.debug("Starting connection");
		MysqlConnectFuture future = new MysqlConnectFuture();
		socketConnector.connect(future);
		
		return future;
	}

	class MysqlConnectFuture extends DefaultDbFuture<Connection> implements IoSessionInitializer<ConnectFuture> {
		private volatile ConnectFuture future;
		private final CountDownLatch latch = new CountDownLatch(1);
		public void initializeSession(IoSession session, ConnectFuture future) {
			logger.trace("Initializing IoSession");
			
			this.future = future;
			latch.countDown();
			
			logger.trace("Initializing session");
			
			final MysqlConnection connection = new MysqlConnection(MysqlConnectionManager.this, this, session, credentials, id.incrementAndGet());
			IoSessionUtil.setMysqlConnection(session, connection);
			synchronized (connections) {
				connections.add(connection);
			}
		}
		@Override
		protected boolean doCancel(boolean mayInterruptIfRunning) {
			logger.trace("Cancelling connect");

			try {
				latch.await();
			} catch (InterruptedException e) {
				return false;
			}
			logger.trace("Acquired IoFuture");
			
			future.cancel();
			boolean canceled = future.isCanceled();
			logger.trace("Cancle result: {}", canceled);
			return canceled;
		}
	}
	
	public void removeConnection(MysqlConnection connection) {
		synchronized (connections) {
			connections.remove(connection);
		}
	}
	
	@Override
	public String toString() {
		InetSocketAddress address = socketConnector.getDefaultRemoteAddress();
		return String.format("%s: mysql://%s:%d/%s (user: %s)", getClass().getName(), address.getHostName(), address.getPort(), credentials.getDatabase(), credentials.getUserName());
	}

}
