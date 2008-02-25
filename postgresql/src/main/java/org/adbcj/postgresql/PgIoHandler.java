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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.adbcj.DbException;
import org.adbcj.Field;
import org.adbcj.Value;
import org.adbcj.postgresql.PgConnectionManager.PgConnectFuture;
import org.adbcj.postgresql.backend.AbstractBackendMessage;
import org.adbcj.postgresql.backend.AuthenticationMessage;
import org.adbcj.postgresql.backend.CommandCompleteMessage;
import org.adbcj.postgresql.backend.DataRowMessage;
import org.adbcj.postgresql.backend.ErrorResponseMessage;
import org.adbcj.postgresql.backend.KeyMessage;
import org.adbcj.postgresql.backend.ReadyMessage;
import org.adbcj.postgresql.backend.RowDescriptionMessage;
import org.adbcj.postgresql.frontend.FrontendMessage;
import org.adbcj.postgresql.frontend.FrontendMessageType;
import org.adbcj.postgresql.frontend.StartupMessage;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultResult;
import org.adbcj.support.AbstractDbSession.Request;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgIoHandler extends IoHandlerAdapter {
	
	private final Logger logger = LoggerFactory.getLogger(PgIoHandler.class);

	private final PgConnectionManager connectionManager;
	
	public PgIoHandler(PgConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("sessionCreated");
	}
	
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		logger.debug("sessionOpened");
		
		
		// Send start message to backend
		logger.trace("Sending start message");
		Map<ConfigurationVariable, String> parameters = new HashMap<ConfigurationVariable, String>();
		parameters.put(ConfigurationVariable.CLIENT_ENCODING, "UNICODE");
		parameters.put(ConfigurationVariable.DATE_STYLE, "ISO");
		session.write(new StartupMessage(connectionManager.getUsername(), connectionManager.getDatabase(), parameters));

	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		logger.debug("IoSession closed");
		PgConnection connection = IoSessionUtil.getConnection(session);
		Request<Void> closeRequest = connection.getCloseRequest();
		if (closeRequest != null) {
			logger.debug("Completing close request");
			closeRequest.complete(null);
		}
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.debug("Exception was thrown", cause);
		PgConnection connection = IoSessionUtil.getConnection(session);
		if (connection != null) {
			DefaultDbFuture<?> future = connection.getConnectFuture();
			if (future != null) {
				errorOutFuture(connection, future, cause);
				return;
			} else {
				Request<?> request = connection.getActiveRequest();
				if (request != null) {
					if (!future.isDone()) {
						try {
							errorOutFuture(connection, future, cause);
							
							return;
						} catch (Exception e) {
							// Hand exception over to connection manager
							e.printStackTrace();
						}
					}
				}
			}
		}
		// Hand exception over to connection manager
		cause.printStackTrace();
	}
	
	private void errorOutFuture(PgConnection connection, DefaultDbFuture<?> future, Throwable cause) {
		if (!future.isDone()) {
			future.setException(DbException.wrap(connection, cause));
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		logger.trace("Message was sent");
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.trace("Received a message {}", message);
		AbstractBackendMessage backendMessage = (AbstractBackendMessage)message;
		// TODO Implement handling remaining backend message types
		switch(backendMessage.getType()) {
		case AUTHENTICATION:
			doAuthentication(session, (AuthenticationMessage)backendMessage);
			break;
		case BIND_COMPLETE:
			logger.trace("Bind complete");
			break;
		case COMMAND_COMPLETE:
			doCommandComplete(session, (CommandCompleteMessage)backendMessage);
			break;
		case DATA_ROW:
			doDataRow(session, (DataRowMessage)backendMessage);
			break;
		case ERROR_RESPONSE:
			doError(session, (ErrorResponseMessage)backendMessage);
			break;
		case KEY:
			doKey(session, (KeyMessage)backendMessage);
			break;
		case NO_DATA:
			logger.trace("No data");
			break;
		case PARAMETER_STATUS:
			// TODO Store parameter values in connection
			break;
		case PARSE_COMPLETE:
			logger.trace("Parse complete");
			break;
		case READY_FOR_QUERY:
			doReadyForQuery(session, (ReadyMessage)backendMessage);
			break;
		case ROW_DESCRIPTION:
			doRowDescription(session, (RowDescriptionMessage)backendMessage);
			break;
		default:
			throw new IllegalStateException("Need to implement handler for messages of type " + backendMessage.getType());	
		}
	}
	
	private void doAuthentication(IoSession session, AuthenticationMessage authenticationMessage) {
		// TODO Support all postgresql authentication types
		switch (authenticationMessage.getAuthenticaitonType()) {
		case OK:
			// Authenticated successfully
			logger.debug("Authenticated successfully");
			break;
		case MD5_PASSWORD:
			byte[] md5hash = PasswordHash.hashPasswordMD5(
					connectionManager.getUsername(),
					connectionManager.getPassword(),
					authenticationMessage.getData());
			FrontendMessage md5Message = new  FrontendMessage(FrontendMessageType.PASSWORD, md5hash);
			session.write(md5Message);
			break;
		default:
			throw new IllegalStateException("We currently do not support the authentication type of " + authenticationMessage.getAuthenticaitonType());
		}
	}

	private void doCommandComplete(IoSession session, CommandCompleteMessage commandCompleteMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a data row without an active request");
		}

		Object accumulator = request.getAccumulator();
		switch (commandCompleteMessage.getCommand()) {
		case SELECT:
			request.getEventHandler().endResults(accumulator);
			request.complete(accumulator);
		case BEGIN:
		case COMMIT:
		case ROLLBACK:
			request.complete(null);
			break;
		case DELETE:
		case INSERT:
		case UPDATE:
			DefaultResult result = new DefaultResult(commandCompleteMessage.getRowCount(), Collections.<String>emptyList());
			request.complete(result);
			break;
		// TODO Implement MOVE command completion
		default:
			throw new IllegalStateException(String.format("Command completions of type %s are not implemented", commandCompleteMessage.getCommand()));
		}
	}

	private void doDataRow(IoSession session, DataRowMessage dataRowMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a data row without an active request");
		}

		request.getEventHandler().startRow(request.getAccumulator());
		for (Value value : dataRowMessage.getValues()) {
			request.getEventHandler().value(value, request.getAccumulator());
		}
		request.getEventHandler().endRow(request.getAccumulator());
	}

	/**
	 * When an error packet is received, a PgException is created and thrown.  The exception is then handled by
	 * {@link #exceptionCaught(IoSession, Throwable)}.
	 * 
	 * @param session  the session under which the exception occurred
	 * @param errorResponseMessage  the message containing the exception
	 */
	private void doError(IoSession session, ErrorResponseMessage errorResponseMessage) {
		// When receiving an error packet, throw exception and let exceptionCaught notify future
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		String message = errorResponseMessage.getFields().get(ErrorField.MESSAGE);
		DbException exception;
		if (message == null) {
			exception = new PgException(connection, errorResponseMessage.getFields());
		} else {
			exception = new PgException(connection, message, errorResponseMessage.getFields());
		}
		throw exception;
	}

	private void doKey(IoSession session, KeyMessage backendMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);

		connection.setKey(backendMessage.getKey());
		connection.setPid(backendMessage.getPid());
	}

	private void doReadyForQuery(IoSession session, ReadyMessage backendMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		// Check if we're doing connection
		PgConnectFuture future = connection.getConnectFuture();
		if (!future.isDone()) {
			logger.debug("Completed connection");
			future.setResult(connection);
			return;
		}
		
		// TODO Determine if we even need to do anything with a ReadyMessage
		switch (backendMessage.getStatus()) {
		case TRANSACTION:
			break;
		case IDLE:
			break;
		case ERROR:
			throw new DbException(connection, "Transaction is in error state");
		default:
			throw new IllegalStateException("Don't know hot to handle backend status of " + backendMessage.getStatus());
		}
	}

	private void doRowDescription(IoSession session, RowDescriptionMessage rowDescriptionMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a row description without an active request");
		}
		
		request.getEventHandler().startFields(request.getAccumulator());
		for (Field field : rowDescriptionMessage.getFields()) {
			request.getEventHandler().field(field, request.getAccumulator());
		}
		request.getEventHandler().endFields(request.getAccumulator());
		request.getEventHandler().startResults(request.getAccumulator());
	}

	public PgConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
}
