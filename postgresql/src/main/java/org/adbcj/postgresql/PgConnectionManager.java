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
package org.adbcj.postgresql;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.postgresql.backend.PgBackendMessageDecoder;
import org.adbcj.postgresql.frontend.PgFrontendMessageEncoder;
import org.adbcj.support.DefaultDbFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionInitializer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgConnectionManager implements ConnectionManager {

	public static final String CODEC_NAME = PgConnectionManager.class.getName() + ".codec";

	private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory() {
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			// TODO Determine if this should be a static instance
			return new PgBackendMessageDecoder();
		}
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			// TODO Determine if this should be a static instance
			return new PgFrontendMessageEncoder();
		}
	};

	private final Logger logger = LoggerFactory.getLogger(PgConnectionManager.class);
	
	private final NioSocketConnector socketConnector;

	private final String username;
	private final String password;
	private final String database;
	
	private DefaultDbFuture<Void> closeFuture = null;

	public PgConnectionManager(String host, int port, String username, String password, String database,
			Properties properties) {
		logger.debug("Creating new PostgresqlConnectionManager");
		
		socketConnector = new NioSocketConnector();

		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();

		filterChain.addLast(CODEC_NAME, new ProtocolCodecFilter(CODEC_FACTORY));
		
		socketConnector.setHandler(new PgIoHandler(this));

		InetSocketAddress address = new InetSocketAddress(host, port);
		socketConnector.setDefaultRemoteAddress(address);
		
		this.username = username;
		this.password = password;
		this.database = database;
	}

	public DbFuture<Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager closed");
		}
		logger.debug("Starting connection");
		PgConnectFuture future = new PgConnectFuture();
		socketConnector.connect(future);
		
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
			logger.debug("Creating PgConnection");

			PgConnection connection = new PgConnection(PgConnectionManager.this, this, session);
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
