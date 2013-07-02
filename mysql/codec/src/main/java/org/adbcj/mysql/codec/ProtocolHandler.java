package org.adbcj.mysql.codec;

import java.util.LinkedList;
import java.util.List;

import org.adbcj.Connection;
import org.adbcj.DbException;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.adbcj.Value;
import org.adbcj.support.AbstractDbSession.Request;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.support.DefaultResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous protocol handler suitable for use with frameworks like MINA or
 * Netty.
 * 
 * @author Mike Heath <mheath@apache.org>
 */
public class ProtocolHandler {

    private final Logger logger = LoggerFactory.getLogger(ProtocolHandler.class);

    public void connectionClosed(AbstractMySqlConnection connection) throws Exception {
        logger.trace("IoSession closed");
        connection.doClose();
    }

    /**
     * Handles an exception
     * 
     * @param connection
     * @param cause
     * @return any exception that couldn't be handled, null if the exception was
     * succesfully handled
     * @throws Exception
     */
    public Throwable handleException(AbstractMySqlConnection connection, Throwable cause) throws Exception {
        logger.debug("Caught exception: ", cause);

        DbException dbException = DbException.wrap(connection, cause);
        if (connection != null) {
            DefaultDbFuture<Connection> connectFuture = connection.getConnectFuture();
            if (!connectFuture.isDone()) {
                connectFuture.setException(dbException);
                return null;
            }
            Request<?> activeRequest = connection.getActiveRequest();
            if (activeRequest != null) {
                if (!activeRequest.isDone()) {
                    try {
                        activeRequest.error(dbException);

                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                }
            }
        }
        return dbException;
    }

    public void messageReceived(AbstractMySqlConnection connection, Object message) throws Exception {
        logger.trace("Received message: {}", message);
        if (message instanceof ServerGreeting) {
            handleServerGreeting(connection, (ServerGreeting) message);
        } else if (message instanceof OkResponse) {
            handleOkResponse(connection, (OkResponse) message);
        } else if (message instanceof ErrorResponse) {
            handleErrorResponse(connection, (ErrorResponse) message);
        } else if (message instanceof ResultSetResponse) {
            handleResultSetResponse(connection, (ResultSetResponse) message);
        } else if (message instanceof ResultSetFieldResponse) {
            handleResultSetFieldResponse(connection, (ResultSetFieldResponse) message);
        } else if (message instanceof ResultSetRowResponse) {
            handleResultSetRowResponse(connection, (ResultSetRowResponse) message);
        } else if (message instanceof EofResponse) {
            handleEofResponse(connection, (EofResponse) message);
        } else {
            throw new IllegalStateException("Unable to handle message of type: " + message.getClass().getName());
        }
    }

    private void handleServerGreeting(AbstractMySqlConnection connection, ServerGreeting serverGreeting) {
        // TODO save the parts of the greeting that we might need (like the
        // protocol version, etc.)
        // Send Login request
        LoginRequest request = new LoginRequest(connection.getCredentials(),
            connection.getClientCapabilities(),
            connection.getExtendedClientCapabilities(),
            connection.getCharacterSet(),
            serverGreeting.getSalt());
        connection.write(request);
    }

    private void handleOkResponse(AbstractMySqlConnection connection, OkResponse response) {
        logger.trace("Response '{}' on connection {}", response, connection);

        List<String> warnings = null;
        if (response.getWarningCount() > 0) {
            warnings = new LinkedList<String>();
            for (int i = 0; i < response.getWarningCount(); i++) {
                warnings.add(response.getMessage());
            }
        }

        logger.warn("Warnings: {}", warnings);

        Request<Result> activeRequest = connection.getActiveRequest();
        if (activeRequest == null) {
            // TODO Do we need to pass the warnings on to the connection?
            DefaultDbFuture<Connection> connectFuture = connection.getConnectFuture();
            if (!connectFuture.isDone()) {
                connectFuture.setResult(connection);

                return;
            } else {
                throw new IllegalStateException("Received an OkResponse with no activeRequest " + response);
            }
        }
        Result result = new DefaultResult(response.getAffectedRows(), warnings);
        activeRequest.complete(result);
    }

    private void handleErrorResponse(AbstractMySqlConnection connection, ErrorResponse message) {
        StringBuilder sb = new StringBuilder();
        if (!message.getMessage().isEmpty()) {
            sb.append(message.getMessage()).append("\n");
        }
        if (!message.getSqlState().isEmpty()) {
            sb.append(message.getSqlState()).append("\n");
        }

        throw new MysqlException(connection, sb.toString());
    }

    private void handleResultSetResponse(AbstractMySqlConnection connection, ResultSetResponse message) {
        Request<ResultSet> activeRequest = connection.getActiveRequest();

        if (activeRequest == null) {
            throw new IllegalStateException("No active request for response: " + message);
        }

        logger.debug("Start field definitions");
        activeRequest.getEventHandler().startFields(activeRequest.getAccumulator());
    }

    private void handleResultSetFieldResponse(AbstractMySqlConnection connection, ResultSetFieldResponse message) {
        Request<ResultSet> activeRequest = connection.getActiveRequest();

        ResultSetFieldResponse fieldResponse = (ResultSetFieldResponse) message;
        activeRequest.getEventHandler().field(fieldResponse.getField(), activeRequest.getAccumulator());
    }

    private void handleResultSetRowResponse(AbstractMySqlConnection connection, ResultSetRowResponse message) {
        Request<ResultSet> activeRequest = connection.getActiveRequest();

        ResultSetRowResponse rowResponse = (ResultSetRowResponse) message;

        activeRequest.getEventHandler().startRow(activeRequest.getAccumulator());
        for (Value value : rowResponse.getValues()) {
            activeRequest.getEventHandler().value(value, activeRequest.getAccumulator());
        }
        activeRequest.getEventHandler().endRow(activeRequest.getAccumulator());
    }

    private void handleEofResponse(AbstractMySqlConnection connection, EofResponse message) {
        logger.trace("Fetching active request in handleEofResponse()");
        Request<ResultSet> activeRequest = connection.getActiveRequest();

        if (activeRequest == null) {
            throw new IllegalStateException("No active request for response: " + message);
        }

        EofResponse eof = (EofResponse) message;
        switch (eof.getType()) {
            case FIELD:
                activeRequest.getEventHandler().endFields(activeRequest.getAccumulator());
                break;
            case ROW:
                activeRequest.getEventHandler().endResults(activeRequest.getAccumulator());
                activeRequest.complete(activeRequest.getAccumulator());
                break;
            default:
                throw new MysqlException(connection, "Unkown eof response type");
        }
    }

}
