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
package org.adbcj.mysql.mina;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.adbcj.Connection;
import org.adbcj.mysql.codec.AbstractMySqlConnectionManager;
import org.adbcj.support.DefaultDbFuture;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlConnectionManager extends AbstractMySqlConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(MysqlConnectionManager.class);

	public static final String CODEC_NAME = MysqlConnectionManager.class.getName() + ".codec";

	private final NioSocketConnector socketConnector;

	private static final ProtocolEncoder ENCODER = new MysqlMessageEncoder();
	private static final ProtocolCodecFactory CODEC_FACTORY = new ProtocolCodecFactory() {
		public ProtocolDecoder getDecoder(IoSession session) throws Exception {
			return new MysqlMessageDecoder();
		}
		public ProtocolEncoder getEncoder(IoSession session) throws Exception {
			return ENCODER;
		}
	};

	public MysqlConnectionManager(String host, int port, String username, String password, String schema, Properties properties) {
		super(username, password, schema, properties);

		socketConnector = new NioSocketConnector();
		//socketConnector.setWorkerTimeout(5); // TODO Make MINA worker timeout configurable in MysqlConnectionManager
		socketConnector.getSessionConfig().setTcpNoDelay(true);
		DefaultIoFilterChainBuilder filterChain = socketConnector.getFilterChain();

		filterChain.addLast(CODEC_NAME, new ProtocolCodecFilter(CODEC_FACTORY));

		socketConnector.setHandler(new MysqlIoHandler(this));
		socketConnector.setDefaultRemoteAddress(new InetSocketAddress(host, port));
	}

	@Override
	protected void dispose() {
		socketConnector.dispose();
	}

	@Override
	protected DefaultDbFuture<Connection> createConnectionFuture() {
		MysqlConnectFuture future = new MysqlConnectFuture();
		socketConnector.connect(future);

		return future;
	}

	class MysqlConnectFuture extends DefaultDbFuture<Connection> implements IoSessionInitializer<ConnectFuture> {
		private boolean done = false;
		private boolean cancelled = false;
		public synchronized void initializeSession(IoSession session, ConnectFuture future) {
			logger.trace("Initializing IoSession");

			// If cancelled, close session and return
			if (cancelled) {
				session.close();
				return;
			}

			// Create MyConnection object and place in IoSession
			final MysqlConnection connection = new MysqlConnection(MysqlConnectionManager.this, this, session, getCredentials());
			IoSessionUtil.setMysqlConnection(session, connection);
		}
		@Override
		protected synchronized boolean doCancel(boolean mayInterruptIfRunning) {
			if (done) {
				return false;
			}
			logger.trace("Cancelling connect");

			cancelled = true;
			return true;
		}
	}

	@Override
	public String toString() {
		InetSocketAddress address = socketConnector.getDefaultRemoteAddress();
		return String.format("%s: mysql://%s:%d/%s (user: %s)", getClass().getName(), address.getHostName(), address.getPort(), getCredentials().getDatabase(), getCredentials().getUserName());
	}

}
