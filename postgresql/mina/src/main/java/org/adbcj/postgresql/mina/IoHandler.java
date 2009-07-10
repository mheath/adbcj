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
package org.adbcj.postgresql.mina;

import org.adbcj.postgresql.codec.backend.AbstractBackendMessage;
import org.adbcj.postgresql.codec.AbstractConnection;
import org.adbcj.postgresql.codec.ProtocolHandler;
import org.adbcj.postgresql.mina.MinaConnectionManager;
import org.adbcj.postgresql.mina.MinaConnection;
import org.adbcj.postgresql.mina.IoSessionUtil;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoHandler extends IoHandlerAdapter {

	private final Logger logger = LoggerFactory.getLogger(IoHandler.class);

	private final ProtocolHandler protocolHandler;

	public IoHandler(MinaConnectionManager connectionManager) {
		protocolHandler = new ProtocolHandler(connectionManager);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("sessionCreated");
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		logger.debug("sessionOpened");

		// Send start message to backend
		AbstractConnection connection = IoSessionUtil.getConnection(session);
		protocolHandler.connectionOpened(connection);

	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.debug("IoSession closed");
		AbstractConnection connection = IoSessionUtil.getConnection(session);
		protocolHandler.closeConnection(connection);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.debug("Exception was thrown", cause);
		MinaConnection connection = IoSessionUtil.getConnection(session);
		protocolHandler.handleException(connection, cause);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		logger.trace("Message was sent");
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.trace("Received a message {}", message);
		MinaConnection connection = IoSessionUtil.getConnection(session);
		protocolHandler.handleMessage(connection, (AbstractBackendMessage)message);
	}

}
