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

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.support.AbstractDbFutureListenerSupport;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.Request;
import org.safehaus.adbcj.support.AbstractTransactionalSession.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlIoHandler extends DemuxingIoHandler {
	
	private final Logger logger = LoggerFactory.getLogger(MysqlIoHandler.class);
	
	@SuppressWarnings("unchecked")
	public MysqlIoHandler() {
		addMessageHandler(ServerGreeting.class, new ServerGreetingMessageHandler());
		addMessageHandler(OkResponse.class, new OkResponseMessageHandler());
		addMessageHandler(ErrorResponse.class, new ErrorResponseMessageHandler());
		
		// Add handler for result set messages
		ResultSetMessagesHandler resultSetMessagesHandler = new ResultSetMessagesHandler();
		addMessageHandler(ResultSetResponse.class, resultSetMessagesHandler);
		addMessageHandler(ResultSetFieldResponse.class, resultSetMessagesHandler);
		addMessageHandler(ResultSetRowResponse.class, resultSetMessagesHandler);
		addMessageHandler(EofResponse.class, resultSetMessagesHandler);
	}
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("IoSession created");
		IoSessionUtil.init(session);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		DefaultDbFuture<Void> closeFuture = connection.getCloseFuture();
		if (closeFuture != null) {
			closeFuture.setDone();
		}
		logger.debug("IoSession closed");
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.debug("Caught exception: ", cause);
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		if (connection != null) {
			Request<?> activeRequest = connection.getActiveRequest();
			if (activeRequest != null) {
				AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
				if (!future.isDone()) {
					try {
						future.setException(DbException.wrap(connection, cause));
						future.setDone();

						Transaction transaction = (Transaction)activeRequest.getTransaction();
						if (transaction != null) {
							transaction.cancelPendingRequests();
						}

						return;
					} catch (Throwable e) {
						// TODO Handle exception in ConnectionManager
						e.printStackTrace();
					} finally {
						connection.makeNextRequestActive();
					}
				}
			}
		}
		// TODO Handle exception in ConnectionManager
		cause.printStackTrace();
	}
	
}
