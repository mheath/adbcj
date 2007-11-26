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

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.Result;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Value;
import org.safehaus.adbcj.support.AbstractDbFutureListenerSupport;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.DefaultResult;
import org.safehaus.adbcj.support.Request;
import org.safehaus.adbcj.support.AbstractTransactionalSession.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlIoHandler extends IoHandlerAdapter {
	
	private final Logger logger = LoggerFactory.getLogger(MysqlIoHandler.class);
	
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

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		if (message instanceof ServerGreeting) {
			handleServerGreeting(session, (ServerGreeting)message);
		} else if (message instanceof OkResponse) {
			handleOkResponse(session, (OkResponse)message);
		} else if (message instanceof ErrorResponse) {
			handleErrorResponse(session, (ErrorResponse)message);
		} else if (message instanceof ResultSetResponse) {
			handleResultSetResponse(session, (ResultSetResponse)message);
		} else if (message instanceof ResultSetFieldResponse) {
			handleResultSetFieldResponse(session, (ResultSetFieldResponse)message);
		} else if (message instanceof ResultSetRowResponse) {
			handleResultSetRowResponse(session, (ResultSetRowResponse)message);
		} else if (message instanceof EofResponse) {
			handleEofResponse(session, (EofResponse)message);
		}
	}

	private void handleServerGreeting(IoSession session, ServerGreeting serverGreeting) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		// Save server greeting
		connection.setServerGreeting(serverGreeting);
		
		// Send Login request
		LoginRequest request = new LoginRequest(connection.getCredentials(), connection.getClientCapabilities(), connection.getExtendedClientCapabilities(), connection.getCharacterSet());
		session.write(request);
	}

	private void handleOkResponse(IoSession session, OkResponse response) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		logger.debug("Response '{}' on connection {}", response, connection);
				
		Request<Result> activeRequest = connection.getActiveRequest();
		if (activeRequest == null) {
			throw new IllegalStateException("Received an OkResponse with no activeRequest " + response);
		}
		DefaultDbFuture<Result> future = activeRequest.getFuture();
		List<String> warnings = null;
		if (response.getWarningCount() > 0) {
			warnings = new LinkedList<String>();
			for (int i = 0; i < response.getWarningCount(); i++) {
				warnings.add(response.getMessage());
			}
		}
		Result result = new DefaultResult(response.getAffectedRows(), warnings);
		future.setValue(result);
		future.setDone();
		connection.makeNextRequestActive();
	}

	private void handleErrorResponse(IoSession session, ErrorResponse message) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		throw new MysqlException(connection, message.getMessage());
	}

	private void handleResultSetResponse(IoSession session, ResultSetResponse message) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		Request<ResultSet> activeRequest = connection.getActiveRequest();

		logger.debug("Start field definitions");
		activeRequest.getEventHandler().startFields(activeRequest.getAccumulator());
	}

	private void handleResultSetFieldResponse(IoSession session, ResultSetFieldResponse message) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		Request<ResultSet> activeRequest = connection.getActiveRequest();

		ResultSetFieldResponse fieldResponse = (ResultSetFieldResponse)message;
		activeRequest.getEventHandler().field(fieldResponse.getField(), activeRequest.getAccumulator());
	}

	private void handleResultSetRowResponse(IoSession session, ResultSetRowResponse message) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		Request<ResultSet> activeRequest = connection.getActiveRequest();

		ResultSetRowResponse rowResponse = (ResultSetRowResponse)message;

		activeRequest.getEventHandler().startRow(activeRequest.getAccumulator());
		for (Value value : rowResponse.getValues()) {
			activeRequest.getEventHandler().value(value, activeRequest.getAccumulator());
		}
		activeRequest.getEventHandler().endRow(activeRequest.getAccumulator());
	}

	private void handleEofResponse(IoSession session, EofResponse message) {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		Request<ResultSet> activeRequest = connection.getActiveRequest();

		EofResponse eof = (EofResponse)message;
		switch (eof.getType()) {
		case FIELD:
			activeRequest.getEventHandler().endFields(activeRequest.getAccumulator());
			break;
		case ROW:
			activeRequest.getEventHandler().endResults(activeRequest.getAccumulator());
			connection.makeNextRequestActive();
			break;
		default:
			throw new MysqlException(connection, "Unkown eof response type");
		}
	}

}
