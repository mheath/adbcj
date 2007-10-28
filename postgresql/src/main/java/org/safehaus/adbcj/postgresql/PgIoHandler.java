package org.safehaus.adbcj.postgresql;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.postgresql.backend.AbstractBackendMessage;
import org.safehaus.adbcj.postgresql.backend.AuthenticationMessage;
import org.safehaus.adbcj.postgresql.backend.ReadyMessage;
import org.safehaus.adbcj.postgresql.frontend.DataMessage;
import org.safehaus.adbcj.postgresql.frontend.FrontendMessageType;
import org.safehaus.adbcj.postgresql.frontend.StartupMessage;
import org.safehaus.adbcj.support.DefaultDbFuture;
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
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.debug("Exception was thrown", cause);
		PgConnection connection = IoSessionUtil.getConnection(session);
		if (connection == null) {
			// TODO Pass exception to connection manager
			cause.printStackTrace();
		} else {
			DefaultDbFuture<?> future = connection.getActiveFuture();
			if (future != null && !future.isDone()) {
				future.setException(DbException.wrap(cause));
				future.setDone();
			} else {
				// TODO Pass exception to connection manager
				cause.printStackTrace();
			}
		}
	}
	
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		logger.trace("Message was sent");
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.debug("Received a message {}", message);
		AbstractBackendMessage backendMessage = (AbstractBackendMessage)message;
		// TODO Implement handling remaining backend message types
		switch(backendMessage.getType()) {
		case AUTHENTICATION:
			doAuthentication(session, (AuthenticationMessage)backendMessage);
			break;
		case KEY:
			// TODO Store pid and key in connection
			break;
		case PARAMETER_STATUS:
			// TODO Store parameter values in connection
			break;
		case READY_FOR_QUERY:
			// TODO If an error state is returned, error our any pending requests in transaction
			doReadyForQuery(session, (ReadyMessage)backendMessage);
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
			logger.trace("Authenticated successfully");
			break;
		case MD5_PASSWORD:
			byte[] md5hash = PasswordHash.hashPasswordMD5(
					connectionManager.getUsername(),
					connectionManager.getDatabase(),
					authenticationMessage.getData());
			DataMessage md5Message = new  DataMessage(FrontendMessageType.PASSWORD, md5hash);
			session.write(md5Message);
			break;
		default:
			throw new IllegalStateException("We currently do not support the authentication type of " + authenticationMessage.getAuthenticaitonType());
		}
	}

	private void doReadyForQuery(IoSession session, ReadyMessage backendMessage) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		DefaultDbFuture<?> future = connection.getActiveFuture();
		if (future == null) {
			throw new IllegalStateException("Received a READY with no current request");
		}
		switch (backendMessage.getStatus()) {
		case TRANSACTION:
			// TODO Check to see if we think we're running in a transaction and if status is not 'T', throw an error
		case IDLE:
			future.setDone();
			break;
		case ERROR:
			// TODO: Make sure that if we're in a transaction, the transaction is canceled
			
			// If the future hasn't already errored out, error it out.
			if (!future.isDone()) {
				future.setException(new DbException("Transaction is in error state"));
				future.setDone();
			}
			break;
		}
	}

	public PgConnectionManager getConnectionManager() {
		return connectionManager;
	}
	
}
