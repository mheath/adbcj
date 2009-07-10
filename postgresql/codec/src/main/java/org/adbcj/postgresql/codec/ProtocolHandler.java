package org.adbcj.postgresql.codec;

import org.adbcj.postgresql.codec.frontend.StartupMessage;
import org.adbcj.postgresql.codec.frontend.SimpleFrontendMessage;
import org.adbcj.postgresql.codec.frontend.FrontendMessageType;
import org.adbcj.postgresql.codec.frontend.AbstractFrontendMessage;
import org.adbcj.postgresql.codec.backend.AbstractBackendMessage;
import org.adbcj.postgresql.codec.backend.AuthenticationMessage;
import org.adbcj.postgresql.codec.backend.CommandCompleteMessage;
import org.adbcj.postgresql.codec.backend.DataRowMessage;
import org.adbcj.postgresql.codec.backend.ErrorResponseMessage;
import org.adbcj.postgresql.codec.backend.KeyMessage;
import org.adbcj.postgresql.codec.backend.ReadyMessage;
import org.adbcj.postgresql.codec.backend.RowDescriptionMessage;
import org.adbcj.support.AbstractDbSession;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultResult;
import org.adbcj.DbException;
import org.adbcj.Value;
import org.adbcj.Field;
import org.adbcj.DbFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * @author Mike Heath
 */
public class ProtocolHandler {

	private final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);

	private final AbstractConnectionManager connectionManager;

	public ProtocolHandler(AbstractConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public void connectionOpened(AbstractConnection connection) {
		Map<ConfigurationVariable, String> parameters = new HashMap<ConfigurationVariable, String>();
		parameters.put(ConfigurationVariable.CLIENT_ENCODING, "UNICODE");
		parameters.put(ConfigurationVariable.DATE_STYLE, "ISO");
		connection.write(new StartupMessage(connectionManager.getUsername(), connectionManager.getDatabase(), parameters));
	}

	public void closeConnection(AbstractConnection connection) {
		AbstractDbSession.Request<Void> closeRequest = connection.getCloseRequest();
		if (closeRequest != null) {
			closeRequest.setResult(null);
		}
		// TODO Make a DbSessionClosedException and use here
		connection.errorPendingRequests(new DbException("Connection closed"));
	}

	public void handleException(AbstractConnection connection, Throwable cause) {
		if (connection != null) {
			DefaultDbFuture<?> future = connection.getConnectFuture();
			if (future != null && !future.isDone()) {
				errorOutFuture(connection, future, cause);
				return;
			} else {
				AbstractDbSession.Request<?> request = connection.getActiveRequest();
				if (request != null) {
					if (!request.isDone()) {
						try {
							errorOutFuture(connection, request, cause);

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

	private void errorOutFuture(AbstractConnection connection, DefaultDbFuture<?> future, Throwable cause) {
		logger.debug("Erroring out future: {}", future);
		if (!future.isDone()) {
			future.setException(DbException.wrap(connection, cause));
		}
	}


	public void handleMessage(AbstractConnection connection, AbstractBackendMessage message) {
		// TODO Implement handling remaining backend message types
		switch(message.getType()) {
		case AUTHENTICATION:
			doAuthentication(connection, (AuthenticationMessage)message);
			break;
		case BIND_COMPLETE:
			logger.trace("Bind complete");
			break;
		case COMMAND_COMPLETE:
			doCommandComplete(connection, (CommandCompleteMessage)message);
			break;
		case DATA_ROW:
			doDataRow(connection, (DataRowMessage)message);
			break;
		case ERROR_RESPONSE:
			doError(connection, (ErrorResponseMessage)message);
			break;
		case KEY:
			doKey(connection, (KeyMessage)message);
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
			doReadyForQuery(connection, (ReadyMessage)message);
			break;
		case ROW_DESCRIPTION:
			doRowDescription(connection, (RowDescriptionMessage)message);
			break;
		default:
			throw new IllegalStateException("Need to implement handler for messages of type " + message.getType());
		}
	}

	private void doAuthentication(AbstractConnection connection, AuthenticationMessage authenticationMessage) {
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
			SimpleFrontendMessage md5Message = new SimpleFrontendMessage(FrontendMessageType.PASSWORD, md5hash);
			connection.write(md5Message);
			break;
		default:
			throw new IllegalStateException("We currently do not support the authentication type of " + authenticationMessage.getAuthenticaitonType());
		}
	}

	private void doCommandComplete(AbstractConnection connection, CommandCompleteMessage commandCompleteMessage) {
		AbstractDbSession.Request<Object> request = connection.getActiveRequest();
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

	private void doDataRow(AbstractConnection connection, DataRowMessage dataRowMessage) {
		AbstractDbSession.Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a data row without an active request");
		}
		logger.debug("Received data row for request {}", request);
		request.getEventHandler().startRow(request.getAccumulator());
		for (Value value : dataRowMessage.getValues()) {
			request.getEventHandler().value(value, request.getAccumulator());
		}
		request.getEventHandler().endRow(request.getAccumulator());
	}

	/**
	 * When an error packet is received, a PgException is created and thrown.  The exception is then handled by
	 * {@link #handleException(AbstractConnection, Throwable)}}
	 *
	 * @param connection  the session under which the exception occurred
	 * @param errorResponseMessage  the message containing the exception
	 */
	private void doError(AbstractConnection connection, ErrorResponseMessage errorResponseMessage) {
		// When receiving an error packet, throw exception and let exceptionCaught notify future
		String message = errorResponseMessage.getFields().get(ErrorField.MESSAGE);
		DbException exception;
		if (message == null) {
			exception = new PgException(connection, errorResponseMessage.getFields());
		} else {
			exception = new PgException(connection, message, errorResponseMessage.getFields());
		}
		throw exception;
	}

	private void doKey(AbstractConnection connection, KeyMessage backendMessage) {
		connection.setKey(backendMessage.getKey());
		connection.setPid(backendMessage.getPid());
	}

	private void doReadyForQuery(AbstractConnection connection, ReadyMessage backendMessage) {
		// Check if we're doing connection
		DefaultDbFuture future = connection.getConnectFuture();
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

	private void doRowDescription(AbstractConnection connection, RowDescriptionMessage rowDescriptionMessage) {
		AbstractDbSession.Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a row description without an active request");
		}
		//logger.debug("Received row description for request {}", request);

		request.getEventHandler().startFields(request.getAccumulator());
		for (Field field : rowDescriptionMessage.getFields()) {
			request.getEventHandler().field(field, request.getAccumulator());
		}
		request.getEventHandler().endFields(request.getAccumulator());
		request.getEventHandler().startResults(request.getAccumulator());
	}

}
