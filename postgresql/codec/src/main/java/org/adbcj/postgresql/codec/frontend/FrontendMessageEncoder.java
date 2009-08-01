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
package org.adbcj.postgresql.codec.frontend;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.adbcj.support.EncoderOutputStream;
import org.adbcj.postgresql.codec.ConnectionState;
import org.adbcj.postgresql.codec.FormatCode;
import org.adbcj.postgresql.codec.ConfigurationVariable;

public class FrontendMessageEncoder {

	private static final byte NULL = (byte)0x00;

	private static Logger logger = LoggerFactory.getLogger(FrontendMessageEncoder.class);

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private final ConnectionState connectionState;

	public FrontendMessageEncoder(ConnectionState connectionState) {
		this.connectionState = connectionState;
	}

	public void encode(OutputStream out, AbstractFrontendMessage... messages) throws IOException {
		for (AbstractFrontendMessage message : messages) {
			encodeMessage(out, message);
		}
	}

	protected void encodeMessage(OutputStream out, AbstractFrontendMessage message)throws IOException {
		logger.trace("Encoding message");

		EncoderOutputStream eos = new EncoderOutputStream(out);

		switch(message.getType()) {
		case FLUSH:
		case PASSWORD:
		case QUERY:
		case SYNC:
		case TERMINATE:
			encodeDataMessage(eos, (SimpleFrontendMessage)message);
			break;
		case BIND:
			encodeBind(eos, (BindMessage)message);
			break;
		case CANCEL_REQUEST:
			encodeCancelRequest(eos, (CancelRequestMessage)message);
			break;
		case CLOSE:
			encodeCloseMessage(eos, (CloseMessage)message);
			break;
		case DESCRIBE:
			encodeDescribeMessage(eos, (DescribeMessage)message);
			break;
		case EXECUTE:
			encodeExecuteMessage(eos, (ExecuteMessage)message);
			break;
		case PARSE:
			encodeParseMessage(eos, (ParseMessage)message);
			break;
		case STARTUP:
			encodeStartupMessage(eos, (StartupMessage)message);
			break;
		default:
			throw new IllegalStateException("Do not know how to encode message of type " + message.getType());
		}
	}

	// TODO Move to MINA module

//	public void dispose(IoSession session) throws Exception {
//		// Nothing to dispose
//	}
//
//	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
//		// TODO Refactor to minimize buffer creation -- There's not reason we can't allocate one buffer for all encoding
//		IoBuffer buffer = null;
//		if (message instanceof AbstractFrontendMessage[]) {
//			AbstractFrontendMessage[] messages = (AbstractFrontendMessage[])message;
//			for (AbstractFrontendMessage msg : messages) {
//				IoBuffer encoding = encodeFrontendMessage(session, msg, out);
//				if (buffer == null) {
//					buffer = encoding;
//					buffer.setAutoExpand(true);
//				} else {
//					encoding.flip();
//					buffer.put(encoding);
//				}
//			}
//		} else {
//			buffer = encodeFrontendMessage(session, (AbstractFrontendMessage)message, out);
//		}
//		buffer.flip();
//		out.write(buffer);
//
//		logger.trace("Flushing");
//		out.flush();
//	}
//
//	public IoBuffer encodeFrontendMessage(IoSession session, AbstractFrontendMessage message, ProtocolEncoderOutput out) throws Exception {
//	}
//
	// === DateMessage encoding =======================================================================================

	private void encodeDataMessage(EncoderOutputStream out, SimpleFrontendMessage message) throws IOException {
		logger.trace("Encoding data message");

		boolean hasData = message.getData() != null;

		int length = 4;
		if (hasData) {
			length += message.getData().length + 1; // Add length of data and null-terminator
		}
		writeLength(out, message.getType(), length);
		if (hasData) {
			out.write(message.getData());
			out.write(NULL);
		}
	}

	// === Bind encoding ==============================================================================================

	private void encodeBind(EncoderOutputStream out, BindMessage message) throws IOException {
		logger.trace("Encoding bind message");

		final int paramFormatsLength = message.getParameterFormats() == null ? 0 : message.getParameterFormats().length;
		final int paramValuesLength = message.getParameterValues() == null ? 0 : message.getParameterValues().length;
		final int resultFormatsLength = message.getResultFormats() == null ? 0 : message.getResultFormats().length;

		byte[] portal = message.getPortal() == null ? EMPTY_BYTE_ARRAY : message.getPortal().getBytes(connectionState.getFrontendCharset());
		byte[] statement = message.getStatement() == null ? EMPTY_BYTE_ARRAY : message.getStatement().getBytes(connectionState.getFrontendCharset());
		byte[] paramValues[] = new byte[paramValuesLength][];
		int valuesLength = 0;
		for (int i = 0; i < paramValuesLength; i++) {
			byte[] value = message.getParameterValues()[i].getBytes(connectionState.getFrontendCharset());
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

		writeLength(out, FrontendMessageType.BIND, length);

		out.write(portal);
		out.write(NULL);
		out.write(statement);
		out.write(NULL);
		out.writeShort((short)paramFormatsLength);
		writeFormatCodes(out, message.getParameterFormats());
		out.writeShort((short)paramValuesLength);
		for (int i = 0; i < paramValuesLength; i++) {
			byte[] value = paramValues[i];
			out.writeInt(value.length);
			out.write(value);
		}
		out.writeShort((short)resultFormatsLength);
		writeFormatCodes(out, message.getResultFormats());
	}

	private void writeFormatCodes(EncoderOutputStream out, FormatCode[] formatCodes) throws IOException {
		if (formatCodes == null) {
			return;
		}
		for (FormatCode formatCode : formatCodes) {
			switch (formatCode) {
				case BINARY:
					out.writeShort((short) 1);
					break;
				case TEXT:
					out.writeShort((short) 0);
					break;
				default:
					throw new IllegalStateException("Unable to encode format code " + formatCode);
			}
		}
	}

	// === Cancel Request Message encoding ============================================================================

	private static final int CANCEL_REQUEST_LENGTH = 16;
	private static final int CANCEL_REQUEST_CODE = 80877102;

	private void encodeCancelRequest(EncoderOutputStream out, CancelRequestMessage cancelRequestMessage) throws IOException {
		logger.trace("Encoding cancel request");

		writeLength(out, FrontendMessageType.CANCEL_REQUEST, CANCEL_REQUEST_LENGTH);
		out.writeInt(CANCEL_REQUEST_CODE);
		out.writeInt(cancelRequestMessage.getProcessId());
		out.writeInt(cancelRequestMessage.getSecretKey());
	}

	// === Close Message encoding =====================================================================================

	private void encodeCloseAndDescribe(EncoderOutputStream out, FrontendMessageType type, byte target, String name) throws IOException {
		byte[] nameBuffer;
		if (name == null) {
			nameBuffer = EMPTY_BYTE_ARRAY;
		} else {
			nameBuffer = name.getBytes(connectionState.getFrontendCharset());
		}

		int length = 4 // length field
			+ 1 // target field
			+ nameBuffer.length + 1; // name field + null terminator

		writeLength(out, type, length);
		out.write(target);
		out.write(nameBuffer);
		out.write(NULL);
	}

	private void encodeCloseMessage(EncoderOutputStream out, CloseMessage message) throws IOException {
		logger.trace("Encoding finalizeClose");

		encodeCloseAndDescribe(out, FrontendMessageType.CLOSE, message.getTarget(), message.getName());
	}

	// === Describe Message encoding ==================================================================================

	private void encodeDescribeMessage(EncoderOutputStream out, DescribeMessage message) throws IOException {
		logger.trace("Encoding describe message");

		encodeCloseAndDescribe(out, FrontendMessageType.DESCRIBE, message.getTarget(), message.getName());
	}

	// === Execute Message encoding ===================================================================================

	private void encodeExecuteMessage(EncoderOutputStream out, ExecuteMessage message) throws IOException {
		logger.trace("Encoding execute message");

		byte[] portal;
		if (message.getPortal() == null) {
			portal = EMPTY_BYTE_ARRAY;
		} else {
			portal = message.getPortal().getBytes(connectionState.getFrontendCharset());
		}

		int length = 4 // length field
			+ portal.length + 1 // portal field + null terminator
			+ 4; // max row count field

		writeLength(out, FrontendMessageType.EXECUTE, length);
		out.write(portal);
		out.write(NULL);
		out.writeInt(message.getMaxRows());
	}

	// === ParseMessage encoding ======================================================================================

	private void encodeParseMessage(EncoderOutputStream out, ParseMessage message) throws IOException {
		logger.trace("Encoding parse message");

		byte[] query = message.getQuery().getBytes(connectionState.getFrontendCharset());
		byte[] statement = message.getStatement() == null ? EMPTY_BYTE_ARRAY : message.getStatement().getBytes(connectionState.getFrontendCharset());

		int parameterCount = message.getParameters() == null ? 0 : message.getParameters().length;
		int length =
			4 // Length field
			+ query.length + 1 // Query plus null-terminator
			+ statement.length + 1 // statement plus null-terminator
			+ 2 // Parameter count
			+ (4 * parameterCount); // OID for each parameter

		writeLength(out, message.getType(), length);
		out.write(statement);
		out.write(NULL);
		out.write(query);
		out.write(NULL);
		out.writeShort((short)parameterCount);
		if (message.getParameters() != null) {
			for (int oid : message.getParameters()) {
				out.writeInt(oid);
			}
		}
	}

	// === Startup Message encoding ===================================================================================

	private static final String USER = "user";
	private static final String DATABASE = "database";

	private void encodeStartupMessage(EncoderOutputStream out, StartupMessage message) throws IOException {
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

		// Convert parametersOut to buffer
		ByteArrayOutputStream parametersOut = new ByteArrayOutputStream();
		for (Map.Entry<ConfigurationVariable, String> entry : message.getParameters().entrySet()) {
			byte[] parameterName = entry.getKey().toString().getBytes(charset);
			byte[] value = entry.getValue().getBytes(charset);
			parametersOut.write(parameterName);
			parametersOut.write(NULL);
			parametersOut.write(value);
			parametersOut.write(NULL);
		}
		byte[] parameters = parametersOut.toByteArray();
		length += parameters.length;
		length++; // Add 1 for final terminating null

		// Put parametersOut into buffer
		writeLength(out, FrontendMessageType.STARTUP, length);
		out.write(StartupMessage.PROTOCOL_VERSION);
		out.write(userProperty);
		out.write(NULL);
		out.write(username);
		out.write(NULL);
		out.write(databaseProperty);
		out.write(NULL);
		out.write(database);
		out.write(NULL);
		out.write(parameters);
		out.write(NULL);
	}

	// === Utility methods ============================================================================================

	private void writeLength(EncoderOutputStream out, FrontendMessageType type, int length) throws IOException {
		out.setLimit(length + 1);
		if (type.includedInPacket()) {
			out.write(type.getValue());
		}
		out.writeInt(length);
	}


}
