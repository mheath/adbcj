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
package org.adbcj.postgresql.codec.backend;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

import org.adbcj.Type;
import org.adbcj.Value;
import org.adbcj.postgresql.codec.PgField;
import org.adbcj.postgresql.codec.FormatCode;
import org.adbcj.postgresql.codec.ErrorField;
import org.adbcj.postgresql.codec.PgFieldType;
import org.adbcj.postgresql.codec.ConfigurationVariable;
import org.adbcj.postgresql.codec.ConnectionState;
import org.adbcj.support.DefaultValue;
import org.adbcj.support.DecoderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes Postgresql backend messages.  Used by Postgresql clients for decoding
 * backend messages.  THis is a stateful decoder and there should be one instance
 * per backend connection.
 */
public class BackendMessageDecoder {

	private final Logger logger = LoggerFactory.getLogger(BackendMessageDecoder.class);
	private static final int MESSAGE_MIN_SIZE = 5;
	private static final int FIELD_LENGTH_SIZE = 4;

	private final ConnectionState connectionState;

	public BackendMessageDecoder(ConnectionState connectionState) {
		this.connectionState = connectionState;
	}

	public AbstractBackendMessage decode(DecoderInputStream input, boolean block) throws IOException {
		// If mark is not support and we can't block, throw an exception
		if (!input.markSupported() && !block) {
			throw new IllegalArgumentException("Non-blocking decoding requires an InputStream that supports marking");
		}
		// TODO This should be the max packet size - make this configurable
		input.mark(Integer.MAX_VALUE);
		AbstractBackendMessage message = null;
		try {
			message = doDecode(input, block);
		} finally {
			if (message == null) {
				input.reset();
			}
		}
		return message;
	}

	private AbstractBackendMessage doDecode(DecoderInputStream input, boolean block) throws IOException {
		logger.trace("Decoding message");
		// If we're in non-blocking mode and we don't have the minimum amount of data to read in a backend message, return null
		if (!block && input.available() < MESSAGE_MIN_SIZE) {
			return null;
		}

		byte typeValue = input.readByte();
		int length = input.readInt() - FIELD_LENGTH_SIZE;  // We don't want to include the length field itself
		// Make sure enough data has been received to process the message
		if (!block && input.available() < length) {
			logger.trace("Need more data");
			return null;
		}
		input.setLimit(length);
		BackendMessageType type = BackendMessageType.fromValue(typeValue);
		// If type is null, throw exception
		if (type == null) {
			throw new IllegalStateException("Do not recognize message of type " + typeValue);
		}

		logger.debug("Decoding message of type {}", type);

		switch (type) {
		// Message types that don't have any extra data
		case BIND_COMPLETE:
		case CLOSE_COMPLETE:
		case COPY_DONE:
		case EMPTY_QUERY_RESPONSE:
		case NO_DATA:
		case PARSE_COMPLETE:
		case PORTAL_SUSPENDED:
			return new SimpleBackendMessage(type);
		case AUTHENTICATION:
			return decodeAuthentication(input);
		case COMMAND_COMPLETE:
			return decodeCommandComplete(input);
		case DATA_ROW:
			return decodeDataRow(input);
		case ERROR_RESPONSE:
			return decodeError(input);
		case KEY:
			return decodeKey(input);
		case PARAMETER_STATUS:
			return decodeParameterStatus(input);
		case READY_FOR_QUERY:
			return decodeReadyForQuery(input);
		case ROW_DESCRIPTION:
			return decodeRowDescription(input);
		case COPY_DATA:
		case COPY_IN_RESPONSE:
		case COPY_OUT_RESPONSE:
		case FUNCTION_CALL_RESPONSE:
		case NOTICE_RESPONSE:
		case NOTIFICATION_RESPONSE:
		case PARAMETER_DESCRIPTION:
		case PASSWORD:
			// TODO Implement decoder for these backend message types
			throw new IllegalStateException("No decoder implemented for message of type " + type);
		default:
			throw new IllegalStateException(String.format("Messages of type %c are not implemented", typeValue));
		}


	}


	private AuthenticationMessage decodeAuthentication(DecoderInputStream input) throws IOException {
		// Get authentication type
		AuthenticationType authenticationType = AuthenticationType.values()[input.readInt()];

		AuthenticationMessage message;
		switch(authenticationType) {
		// Authentication types that don't have a payload
		case OK:
		case KERBEROS_5:
		case CLEARTEXT_PASSWORD:
		case SCM_CREDENTIAL:
		case GSS:
			return new AuthenticationMessage(authenticationType);
		// Get crypt salt
		case CRYPT_PASSWORD:
			byte[] cryptSalt = new byte[2];
			input.read(cryptSalt);
			return new AuthenticationMessage(authenticationType, cryptSalt);
		// Get md5 salt
		case MD5_PASSWORD:
			byte[] md5Salt = new byte[FIELD_LENGTH_SIZE];
			input.read(md5Salt);
			return new AuthenticationMessage(authenticationType, md5Salt);
		// Get GSSAPI authentication data
		case GSS_CONTINUE:
			byte[] data = new byte[input.getLimit()];
			input.read(data);
			return new AuthenticationMessage(authenticationType, data);
		case UNKNOWN:
		default:
			throw new IllegalStateException("Don't know how to handle authentication type of " + authenticationType);
		}
	}

	private static final Pattern COMMAND_PATTERN = Pattern.compile("(\\w+)\\s*(\\d*)\\s*(\\d*)");

	private CommandCompleteMessage decodeCommandComplete(DecoderInputStream input) throws IOException {
		Charset charset = connectionState.getBackendCharset();

		String commandStr = input.readString(charset);
		Matcher matcher = COMMAND_PATTERN.matcher(commandStr);

		if (!matcher.matches()) {
			throw new IllegalStateException(String.format("Unable to parse command completion string '%s'", commandStr));
		}
		Command command = Command.valueOf(matcher.group(1));
		long count = -1;
		int oid = -1;
		if (matcher.group(3).length() > 0) {
			oid = Integer.valueOf(matcher.group(2));
			count = Long.valueOf(matcher.group(3));
		} else if (matcher.group(2).length() > 0) {
			count = Long.valueOf(matcher.group(2));
		}

		return new CommandCompleteMessage(command, count, oid);
	}

	private DataRowMessage decodeDataRow(DecoderInputStream input) throws IOException {
		Charset charset = connectionState.getBackendCharset();
		PgField[] fields = connectionState.getCurrentResultSetFields();
		if (fields == null) {
			throw new IllegalStateException("Received a data row without any field definitions in the request payload");
		}

		int fieldCount = input.readUnsignedShort();
		Value[] values = new Value[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			int valueLength = input.readInt();
			PgField field = fields[i];
			Value value;
			if (valueLength < 0) {
				value = new DefaultValue(field, null);
			} else {
				String strVal;
				switch (field.getColumnType()) {
				case INTEGER:
					switch (field.getFormatCode()) {
					case BINARY:
						value = new DefaultValue(field, input.readInt());
						break;
					case TEXT:
						strVal = input.readString(valueLength, charset);
						value = new DefaultValue(field, Integer.valueOf(strVal));
						break;
					default:
						throw new IllegalStateException("Unable to decode format of " + field.getFormatCode());
					}
					break;
				case VARCHAR:
					strVal = input.readString(valueLength, charset);
					value = new DefaultValue(field, strVal);
					break;
				default:
					// Advance buffer
					input.skip(valueLength);
					// TODO Handle remaining ADBCJ types
					throw new IllegalStateException("Unable to decode column of type " + field.getColumnType());
				}
			}
			values[i] = value;
		}
		return new DataRowMessage(values);
	}

	private AbstractBackendMessage decodeError(DecoderInputStream input) throws IOException {
		Map<ErrorField, String> fields = new HashMap<ErrorField, String>();
		for(;;) {
			byte token = input.readByte();
			if (token == 0) {
				break;
			}
			ErrorField field = ErrorField.toErrorField(token);
			String value = input.readString(connectionState.getBackendCharset());
			if (field == null) {
				logger.warn("Unrecognized error field of type '{}' with the value '{}'", (char)token, value);
			} else {
				fields.put(field, value);
			}
		}
		return new ErrorResponseMessage(fields);
	}

	private AbstractBackendMessage decodeKey(DecoderInputStream input) throws IOException {
		int pid = input.readInt();
		int key = input.readInt();
		return new KeyMessage(pid, key);
	}

	private AbstractBackendMessage decodeParameterStatus(DecoderInputStream input) throws IOException {
		Charset charset = connectionState.getBackendCharset();
		String name = input.readString(charset);
		String value = input.readString(charset);
		ConfigurationVariable cv = ConfigurationVariable.fromName(name);
		if (cv == null) {
			logger.warn("No ConfigurationVariable entry for {}", name);
		}
		return new ParameterMessage(cv, value);
	}

	private ReadyMessage decodeReadyForQuery(DecoderInputStream input) throws IOException {
		char s = (char)input.readByte();
		Status status;
		switch(s) {
		case 'E':
			status = Status.ERROR;
			break;
		case 'I':
			status = Status.IDLE;
			break;
		case 'T':
			status = Status.TRANSACTION;
			break;
		default:
			throw new IllegalStateException("Unrecognized server status " + s);
		}
		return new ReadyMessage(status);
	}

	private AbstractBackendMessage decodeRowDescription(DecoderInputStream input) throws IOException {
		Charset charset = connectionState.getBackendCharset();

		int fieldCount = input.readUnsignedShort();
		PgField[] fields = new PgField[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			String name = input.readString(charset);
			int tableOid = input.readInt();
			int columnAttributeNumber = input.readUnsignedShort();
			int typeOid = input.readInt();
			short typeSize = input.readShort();
			int typeModifier = input.readInt();
			FormatCode code = FormatCode.values()[input.readShort()];

			Type type;
			switch (typeOid) {
			case PgFieldType.BOOLEAN:
				type = Type.BOOLEAN;
				break;
			case PgFieldType.BIGINT:
				type = Type.BIGINT;
				break;
			case PgFieldType.CHAR:
				type = Type.CHAR;
				break;
			case PgFieldType.DATE:
				type = Type.DATE;
				break;
			case PgFieldType.DOUBLE:
				type = Type.DOUBLE;
				break;
			case PgFieldType.INTEGER:
				type = Type.INTEGER;
				break;
			case PgFieldType.REAL:
				type = Type.REAL;
				break;
			case PgFieldType.SMALLINT:
				type = Type.SMALLINT;
				break;
			case PgFieldType.VARCHAR:
				type = Type.VARCHAR;
				break;
			default:
				// TODO Convert more typeOids to ADBCJ types
				throw new IllegalStateException("Unable to handle field type with oid " + typeOid);
			}

			fields[i] = new PgField(
					i,
					connectionState.getDatabaseName(),
					type,
					name,
					tableOid,
					columnAttributeNumber,
					code,
					typeSize,
					typeModifier
					);
			logger.debug("Setting fields for current result set: {}", fields);
			connectionState.setCurrentResultSetFields(fields);
		}

		return new RowDescriptionMessage(fields);
	}

}
