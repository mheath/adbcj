package org.safehaus.adbcj.postgresql.frontend;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.safehaus.adbcj.postgresql.ConfigurationVariable;
import org.safehaus.adbcj.postgresql.FormatCode;
import org.safehaus.adbcj.postgresql.IoSessionUtil;
import org.safehaus.adbcj.postgresql.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgFrontendMessageEncoder implements ProtocolEncoder {
	
	private static final byte NULL = (byte)0x00;

	private Logger logger = LoggerFactory.getLogger(PgFrontendMessageEncoder.class);

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	public void dispose(IoSession session) throws Exception {
		// Nothing to dispose
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		logger.trace("Encoding message");
		
		AbstractFrontendMessage frontendMessage = (AbstractFrontendMessage)message;
		IoBuffer buffer;
		switch(frontendMessage.getType()) {
		case FLUSH:
		case PASSWORD:
		case QUERY:
		case SYNC:
		case TERMINATE:
			buffer = encodeDataMessage(session, (FrontendMessage)frontendMessage);
			break;
		case BIND:
			buffer = encodeBind(session, (BindMessage)frontendMessage);
			break;
		case CANCEL_REQUEST:
			buffer = encodeCancelRequest(session, (CancelRequestMessage)frontendMessage);
			break;
		case CLOSE:
			buffer = encodeCloseMessage(session, (CloseMessage)frontendMessage);
			break;
		case DESCRIBE:
			buffer = encodeDescribeMessage(session, (DescribeMessage)frontendMessage);
			break;
		case EXECUTE:
			buffer = encodeExecuteMessage(session, (ExecuteMessage)frontendMessage);
			break;
		case PARSE:
			buffer = encodeParseMessage(session, (ParseMessage)frontendMessage);
			break;
		case STARTUP:
			buffer = encodeStartupMessage(session, (StartupMessage)frontendMessage);
			break;
		default:
			throw new IllegalStateException("Do not know how to encode message of type " + frontendMessage.getType());
		}

		if (buffer.hasRemaining()) {
			throw new IllegalStateException(String.format("Message buffer for %s is not full", frontendMessage.getType()));
		}
		
		buffer.flip();
		out.write(buffer);
		logger.trace("Encoding complete");
	}

	// === DateMessage encoding =======================================================================================
	
	private IoBuffer encodeDataMessage(IoSession session, FrontendMessage frontendMessage) {
		logger.trace("Encoding data message");
		
		boolean hasData = frontendMessage.getData() != null;

		int length = 4;
		if (hasData) {
			length += frontendMessage.getData().length + 1; // Add length of data and null-terminator
		}
		IoBuffer buffer = createBuffer(frontendMessage.getType(), length);
		if (hasData) {
			buffer.put(frontendMessage.getData()).put(NULL);
		}
		
		return buffer;
	}

	// === Bind encoding ==============================================================================================
	
	private IoBuffer encodeBind(IoSession session, BindMessage bindMessage) {
		logger.trace("Encoding bind message");
		
		PgConnection connection = IoSessionUtil.getConnection(session);
		Charset charset = connection.getFrontendCharset();
		
		final int paramFormatsLength = bindMessage.getParameterFormats() == null ? 0 : bindMessage.getParameterFormats().length;
		final int paramValuesLength = bindMessage.getParameterValues() == null ? 0 : bindMessage.getParameterValues().length;
		final int resultFormatsLength = bindMessage.getResultFormats() == null ? 0 : bindMessage.getResultFormats().length;
		
		byte[] portal = bindMessage.getPortal() == null ? EMPTY_BYTE_ARRAY : bindMessage.getPortal().getBytes(charset);
		byte[] statement = bindMessage.getStatement() == null ? EMPTY_BYTE_ARRAY : bindMessage.getStatement().getBytes(charset);
		byte[] paramValues[] = new byte[paramValuesLength][];
		int valuesLength = 0;
		for (int i = 0; i < paramValuesLength; i++) {
			byte[] value = bindMessage.getParameterValues()[i].getBytes(charset);
			paramValues[i] = value;
			valuesLength += 4 + value.length;
		}
		
		int length = 4 // Length field 
			+ portal.length + 1 // Portal name field plus null-termination
			+ statement.length + 1 // Statement name field plus null-termination
			+ 2 // Format parameter codes count field
			+ paramFormatsLength * 2 // The parameter formats (2 bytes each)
			+ 2 // Parameter values count field
			+ valuesLength // Parameter value lengths added below
			+ 2 // Result-column format codes count field
			+ resultFormatsLength * 2; // The result-column formats (2 bytes each)
		
		IoBuffer buffer = createBuffer(FrontendMessageType.BIND, length);
		
		buffer.put(portal).put(NULL);
		buffer.put(statement).put(NULL);
		buffer.putShort((short)paramFormatsLength);
		putFormatCodes(bindMessage.getParameterFormats(), buffer);
		buffer.putShort((short)paramValuesLength);
		for (int i = 0; i < paramValuesLength; i++) {
			byte[] value = paramValues[i];
			buffer.putInt(value.length);
			buffer.put(value);
		}
		buffer.putShort((short)resultFormatsLength);
		putFormatCodes(bindMessage.getResultFormats(), buffer);
		return buffer;
	}

	private void putFormatCodes(FormatCode[] formatCodes, IoBuffer buffer) {
		if (formatCodes == null) {
			return;
		}
		for (int i = 0; i < formatCodes.length; i++) {
			FormatCode formatCode = formatCodes[i];
			switch (formatCode) {
			case BINARY:
				buffer.putShort((short)1);
				break;
			case TEXT:
				buffer.putShort((short)0);
				break;
			default:
				throw new IllegalStateException("Unable to encode format code " + formatCode);	
			}
		}
	}

	// === Cancel Request Message encoding ============================================================================

	private static final int CANCEL_REQUEST_LENGTH = 16;
	private static final int CANCEL_REQUEST_CODE = 80877102;
	
	private IoBuffer encodeCancelRequest(IoSession session, CancelRequestMessage cancelRequestMessage) {
		logger.trace("Encoding cancel request");
		
		IoBuffer buffer = createBuffer(FrontendMessageType.CANCEL_REQUEST, CANCEL_REQUEST_LENGTH);
		buffer.putInt(CANCEL_REQUEST_CODE);
		buffer.putInt(cancelRequestMessage.getProcessId());
		buffer.putInt(cancelRequestMessage.getSecretKey());
		return buffer;
	}

	// === Close Message encoding =====================================================================================
	
	private IoBuffer encodeCloseAndDescribe(IoSession session, FrontendMessageType type, byte target, String name) {
		PgConnection connection = IoSessionUtil.getConnection(session);
		Charset charset = connection.getFrontendCharset();
		
		byte[] nameBuffer;
		if (name == null) {
			nameBuffer = EMPTY_BYTE_ARRAY;
		} else {
			nameBuffer = name.getBytes(charset);
		}
		
		int length = 4 // length field
			+ 1 // target field
			+ nameBuffer.length + 1; // name field + null terminator
		
		IoBuffer buffer = createBuffer(type, length);
		buffer.put(target);
		buffer.put(nameBuffer).put(NULL);
		
		return buffer;
	}

	private IoBuffer encodeCloseMessage(IoSession session, CloseMessage closeMessage) {
		logger.trace("Encoding close");
		
		return encodeCloseAndDescribe(session, FrontendMessageType.CLOSE, closeMessage.getTarget(), closeMessage.getName());
	}

	// === Describe Message encoding ==================================================================================
	
	private IoBuffer encodeDescribeMessage(IoSession session, DescribeMessage describeMessage) {
		logger.trace("Encoding describe message");
		
		return encodeCloseAndDescribe(session, FrontendMessageType.DESCRIBE, describeMessage.getTarget(), describeMessage.getName());
	}
	
	// === Execute Message encoding ===================================================================================
	
	private IoBuffer encodeExecuteMessage(IoSession session, ExecuteMessage executeMessage) {
		logger.trace("Encoding execute message");

		PgConnection connection = IoSessionUtil.getConnection(session);
		Charset charset = connection.getFrontendCharset();

		byte[] portal;
		if (executeMessage.getPortal() == null) {
			portal = EMPTY_BYTE_ARRAY;
		} else {
			portal = executeMessage.getPortal().getBytes(charset);
		}

		int length = 4 // length field
			+ portal.length + 1 // portal field + null terminator
			+ 4; // max row count field
		
		IoBuffer buffer = createBuffer(FrontendMessageType.EXECUTE, length);
		buffer.put(portal).put(NULL);
		buffer.putInt(executeMessage.getMaxRows());
		
		return buffer;
	}

	// === ParseMessage encoding ======================================================================================

	private IoBuffer encodeParseMessage(IoSession session, ParseMessage parseMessage) {
		logger.trace("Encoding parse message");
		
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		byte[] query = parseMessage.getQuery().getBytes(connection.getFrontendCharset());
		byte[] statement = parseMessage.getStatement() == null ? EMPTY_BYTE_ARRAY : parseMessage.getStatement().getBytes(connection.getFrontendCharset());
		
		int parameterCount = parseMessage.getParameters() == null ? 0 : parseMessage.getParameters().length;
		int length =
			4 // Length field
			+ query.length + 1 // Query plus null-terminator
			+ statement.length + 1 // statement plus null-terminator
			+ 2 // Parameter count
			+ (4 * parameterCount); // OID for each parameter
		
		IoBuffer buffer = createBuffer(parseMessage.getType(), length);
		buffer.put(statement).put(NULL);
		buffer.put(query).put(NULL);
		buffer.putShort((short)parameterCount);
		if (parseMessage.getParameters() != null) {
			for (int oid : parseMessage.getParameters()) {
				buffer.putInt(oid);
			}
		}
		
		return buffer;
	}

	// === Startup Message encoding ===================================================================================
	
	private static final String USER = "user";
	private static final String DATABASE = "database";
	
	private IoBuffer encodeStartupMessage(IoSession session, StartupMessage message) {
		logger.trace("Encoding startup message");

		// Use ASCII for encoding startup messages
		Charset charset = Charset.forName("US-ASCII");
		
		byte[] userProperty = USER.getBytes(charset);
		byte[] username = message.getUsername().getBytes(charset); 
		byte[] databaseProperty = DATABASE.getBytes(charset);
		byte[] database = message.getDatabase().getBytes(charset);
		
		int length = 4 // Length field
				+ StartupMessage.PROTOCOL_VERSION.length // version field
				+ userProperty.length + 1 // user parameter + null termination
				+ username.length + 1 // username + null termination
				+ databaseProperty.length + 1 // database parameter + null termination
				+ database.length + 1; // database name + null termination

		// Convert parameters to buffer
		IoBuffer parameters = IoBuffer.allocate(1024);
		for (Map.Entry<ConfigurationVariable, String> entry : message.getParameters().entrySet()) {
			byte[] parameterName = entry.getKey().toString().getBytes(charset);
			byte[] value = entry.getValue().getBytes(charset);
			parameters.put(parameterName).put(NULL);
			parameters.put(value).put(NULL);
		}
		parameters.flip();
		length += parameters.limit();
		length++; // Add 1 for final terminating null
		
		// Put parameters into buffer
		IoBuffer buffer = createBuffer(FrontendMessageType.STARTUP, length);
		buffer.put(StartupMessage.PROTOCOL_VERSION);
		buffer.put(userProperty).put(NULL);
		buffer.put(username).put(NULL);
		buffer.put(databaseProperty).put(NULL);
		buffer.put(database).put(NULL);
		buffer.put(parameters);
		buffer.put(NULL);

		return buffer;
	}

	// === Utility methods ============================================================================================
	
	private IoBuffer createBuffer(FrontendMessageType type, int length) {
		IoBuffer buffer;
		if (type.includedInPacket()) {
			buffer = IoBuffer.allocate(length + 1);
			buffer.put(type.getValue());
		} else {
			buffer = IoBuffer.allocate(length);
		}
		buffer.setAutoExpand(false);
		buffer.putInt(length);
		return buffer;
	}
	
}
