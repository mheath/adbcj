package org.safehaus.adbcj.postgresql.frontend;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.safehaus.adbcj.postgresql.ConfigurationVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgFrontendMessageEncoder implements ProtocolEncoder {
	
	private Logger logger = LoggerFactory.getLogger(PgFrontendMessageEncoder.class);

	public void dispose(IoSession session) throws Exception {
		// Nothing to dispose
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		logger.trace("Encoding message");
		
		AbstractFrontendMessage frontendMessage = (AbstractFrontendMessage)message;
		switch(frontendMessage.getType()) {
		case PASSWORD:
			encodeDataMessage(session, (DataMessage)frontendMessage, out);
			break;
		case STARTUP:
			encodeStartupMessage(session, (StartupMessage)frontendMessage, out);
			break;
		default:
			throw new IllegalStateException("Do not know how to encode message of type " + frontendMessage.getType());
		}
		logger.trace("Encoding complete");
	}

	// === Startup Message encoding ===================================================================================
	
	private static final String USER = "user";
	private static final String DATABASE = "database";
	
	private void encodeStartupMessage(IoSession session, StartupMessage message, ProtocolEncoderOutput out) {
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
			parameters.put(parameterName).put((byte)0x00);
			parameters.put(value).put((byte)0x00);
		}
		parameters.flip();
		length += parameters.limit();
		length++; // Add 1 for final terminating null
		
		// Put parameters into buffer
		IoBuffer buffer = createBuffer(FrontendMessageType.STARTUP, length);
		buffer.put(StartupMessage.PROTOCOL_VERSION);
		buffer.put(userProperty).put((byte)0x00);
		buffer.put(username).put((byte)0x00);
		buffer.put(databaseProperty).put((byte)0x00);
		buffer.put(database).put((byte)0x00);
		buffer.put(parameters);
		buffer.put((byte)0x00);
		
		checkBuffer(buffer);
		out.write(buffer);
	}

	// === DateMessage encoding =======================================================================================
	
	private void encodeDataMessage(IoSession session, DataMessage frontendMessage, ProtocolEncoderOutput out) {
		logger.trace("Encoding data message");
		
		int length = 4 + frontendMessage.getData().length + 1; // length field + data length + null termination
		IoBuffer buffer = createBuffer(frontendMessage.getType(), length);
		buffer.put(frontendMessage.getData()).put((byte)0x00);
		checkBuffer(buffer);
		out.write(buffer);
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
	
	private void checkBuffer(IoBuffer buffer) {
		if (buffer.hasRemaining()) {
			throw new IllegalStateException("Startup message buffer is not full");
		}
		
		buffer.flip();
	}

}
