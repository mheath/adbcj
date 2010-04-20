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

import org.adbcj.mysql.codec.ProtocolHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlIoHandler extends IoHandlerAdapter {

	private final Logger logger = LoggerFactory.getLogger(MysqlIoHandler.class);

	private final ProtocolHandler handler = new ProtocolHandler();

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.trace("IoSession created");
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.trace("IoSession closed");
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		handler.connectionClosed(connection);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.debug("Caught exception: ", cause);
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		Throwable e = handler.handleException(connection, cause);
		if (e != null) {
			// TODO: Pass exception on to connectionManager for handling
			e.printStackTrace();
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.debug("Received message: {}", message);
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		handler.messageReceived(connection, message);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		logger.trace("Message sent: {}", message);
	}

}
