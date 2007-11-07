package org.safehaus.adbcj.postgresql.backend;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.safehaus.adbcj.Type;
import org.safehaus.adbcj.Value;
import org.safehaus.adbcj.postgresql.ConfigurationVariable;
import org.safehaus.adbcj.postgresql.ErrorField;
import org.safehaus.adbcj.postgresql.FormatCode;
import org.safehaus.adbcj.postgresql.IoSessionUtil;
import org.safehaus.adbcj.postgresql.PgConnection;
import org.safehaus.adbcj.postgresql.PgField;
import org.safehaus.adbcj.postgresql.PgFieldType;
import org.safehaus.adbcj.postgresql.PgIoHandler;
import org.safehaus.adbcj.postgresql.PgResultSet;
import org.safehaus.adbcj.support.DefaultValue;
import org.safehaus.adbcj.support.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgBackendMessageDecoder extends CumulativeProtocolDecoder {

	private final Logger logger = LoggerFactory.getLogger(PgBackendMessageDecoder.class);
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		logger.trace("Decoding message");
		// TODO Make MySQL decoder more like postgresql decoder
		
		// Check to see if we have enough data to read the message type and message length
		if (in.remaining() < 5) {
			return false;
		}

		// Check to see if we've read the entire message
		int length;
		try {
			in.mark();
			in.get(); // Throw away type
			length = in.getInt() - 4;  // Subtract 4 because we don't want to include the length field itself
			if (in.remaining() < length) {
				logger.trace("Need more data");
				return false;
			}
		} finally {
			in.reset();
		}
		
		// Get the type and length
		byte typeValue = in.get();
		BackendMessageType type = BackendMessageType.fromValue(typeValue);
		in.getInt(); // Throw away length because we've already fetched it
		
		// Create a buffer for just the current message being processed
		IoBuffer buffer = in.duplicate();
		buffer.limit(buffer.position() + length);
		in.skip(length); // Skip the bytes the will be processed with 'buffer'
		
		// If type is null, throw exception
		if (type == null) {
			throw new IllegalStateException("Don't recognize message of type " + typeValue);
		}
		
		logger.debug("Decoding message of type {}", type);
		
		// TODO Implement decoder for remaining backend message types
		switch (type) {
		// Message types that don't have any extra data
		case BIND_COMPLETE:
		case CLOSE_COMPLETE:
		case COPY_DONE:
		case EMPTY_QUERY_RESPONSE:
		case NO_DATA:
		case PARSE_COMPLETE:
		case PORTAL_SUSPENDED:
			out.write(new BackendMessage(type));
			break;
		case AUTHENTICATION:
			decodeAuthentication(session, buffer, out);
			break;
		case COMMAND_COMPLETE:
			decodeCommandComplete(session, buffer, out);
			break;
		case DATA_ROW:
			decodeDataRow(session, buffer, out);
			break;
		case ERROR_RESPONSE:
			decodeError(session, buffer, out);
			break;
		case KEY:
			decodeKey(session, buffer, out);
			break;
		case PARAMETER_STATUS:
			decodeParameterStatus(session, buffer, out);
			break;
		case READY_FOR_QUERY:
			decodeReadyForQuery(session, buffer, out);
			break;
		case ROW_DESCRIPTION:
			decodeRowDescription(session, buffer, out);
			break;
		default:
			throw new IllegalStateException(String.format("Messages of type %s are not implemented", type)); 
		}

		if (buffer.hasRemaining()) {
			throw new IllegalStateException(String.format("buffer has %d unread bytes after decoding message of type %s", buffer.remaining(), type));
		}
		
		return in.hasRemaining();
	}

	private void decodeAuthentication(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) {
		// Get authentication type
		AuthenticationType authenticationType = buffer.getEnumInt(AuthenticationType.class);
		
		AuthenticationMessage message;
		switch(authenticationType) {
		// Authentication types that don't have a payload
		case OK:
		case KERBEROS_5:
		case CLEARTEXT_PASSWORD:
		case SCM_CREDENTIAL:
		case GSS:
			message = new AuthenticationMessage(authenticationType);
			break;
		// Get crypt salt
		case CRYPT_PASSWORD:
			byte[] cryptSalt = new byte[2];
			buffer.get(cryptSalt);
			message = new AuthenticationMessage(authenticationType, cryptSalt);
			break;
		// Get md5 salt
		case MD5_PASSWORD:
			byte[] md5Salt = new byte[4];
			buffer.get(md5Salt);
			message = new AuthenticationMessage(authenticationType, md5Salt);
			break;
		// Get GSSAPI authentication data
		case GSS_CONTINUE:
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			message = new AuthenticationMessage(authenticationType, data);
			break;
		case UNKNOWN:
		default:
			throw new IllegalStateException("Don't know how to handle authentication type of " + authenticationType);
		}
		
		out.write(message);
	}

	private static final Pattern COMMAND_PATTERN = Pattern.compile("(\\w+)\\s*(\\d*)\\s*(\\d*)"); 
	
	private void decodeCommandComplete(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws CharacterCodingException {
		PgConnection connection = IoSessionUtil.getConnection(session);
		CharsetDecoder decoder = connection.getBackendCharset().newDecoder();

		String commandStr = buffer.getString(decoder);
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
		
		CommandCompleteMessage message = new CommandCompleteMessage(command, count, oid);
		out.write(message);
	}

	private void decodeDataRow(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws CharacterCodingException {
		PgConnection connection = IoSessionUtil.getConnection(session);
		CharsetDecoder decoder = connection.getBackendCharset().newDecoder();
		
		Request<Object> request = connection.getActiveRequest();
		if (request == null) {
			throw new IllegalStateException("Received a data row without an active request");
		}
		PgResultSet resultSet = (PgResultSet)request.getPayload();
		if (resultSet == null) {
			throw new IllegalStateException("Received a data row without an active result set");
		}

		int fieldCount = buffer.getUnsignedShort();
		Value[] values = new Value[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			int valueLength = buffer.getInt();
			PgField field = (PgField)resultSet.getFields().get(i);
			Value value;
			if (valueLength < 0) {
				value = new DefaultValue(field, null);
			} else {
				// TODO Convert raw data to value
				String strVal;
				switch (field.getColumnType()) {
				case INTEGER:
					// TODO Modify to use getString that doesn't terminate on null
					strVal = buffer.getString(valueLength, decoder);
					value = new DefaultValue(field, Integer.valueOf(strVal));
					break;
				case VARCHAR:
					// TODO Modify to use getString that doesn't terminate on null
					strVal = buffer.getString(valueLength, decoder);
					value = new DefaultValue(field, strVal);
					break;
				default:
					// Advance buffer
					byte[] data = new byte[valueLength];
					buffer.get(data);
					// TODO Handle remaining ADBCJ types
					throw new IllegalStateException("Unable to decode field type of " + field.getColumnType());
				}
			}
			values[i] = value; 
		}
		out.write(new DataRowMessage(values));
	}

	private void decodeError(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws CharacterCodingException {
		PgConnection connection = IoSessionUtil.getConnection(session);
		
		Map<ErrorField, String> fields = new HashMap<ErrorField, String>();
		for(;;) {
			byte token = buffer.get();
			if (token == 0) {
				break;
			}
			ErrorField field = ErrorField.toErrorField(token);
			String value = buffer.getString(connection.getBackendCharset().newDecoder());
			if (field == null) {
				logger.warn("Unrecognized error field of type '{}' with the value 'value'", Character.valueOf((char)token), value);
			} else {
				fields.put(field, value);
			}
		}
		ErrorResponseMessage message = new ErrorResponseMessage(fields);
		out.write(message);
	}

	private void decodeKey(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) {
		int pid = buffer.getInt();
		int key = buffer.getInt();
		KeyMessage message = new KeyMessage(pid, key);
		out.write(message);
	}

	private void decodeParameterStatus(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws CharacterCodingException {
		PgConnection connection = IoSessionUtil.getConnection(session);
		CharsetDecoder decoder = connection.getBackendCharset().newDecoder();
		String name = buffer.getString(decoder);
		String value = buffer.getString(decoder);
		ConfigurationVariable cv = ConfigurationVariable.fromName(name);
		if (cv == null) {
			logger.warn("No ConfigurationVariable entry for {}", name);
		}
		ParameterMessage message = new ParameterMessage(cv, value);
		out.write(message);
	}

	private void decodeReadyForQuery(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) {
		char s = (char)buffer.get();
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
		ReadyMessage message = new ReadyMessage(status);
		out.write(message);
	}

	private void decodeRowDescription(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws CharacterCodingException {
		PgIoHandler ioHandler = (PgIoHandler)session.getHandler();
		PgConnection connection = IoSessionUtil.getConnection(session);
		CharsetDecoder decoder = connection.getBackendCharset().newDecoder();

		int fieldCount = buffer.getUnsignedShort();
		PgResultSet resultSet = new PgResultSet(connection, fieldCount);
		PgField[] fields = new PgField[fieldCount];
		for (int i = 0; i < fieldCount; i++) {
			String name = buffer.getString(decoder);
			int tableOid = buffer.getInt();
			int columnAttributeNumber = buffer.getUnsignedShort();
			int typeOid = buffer.getInt();
			short typeSize = buffer.getShort();
			int typeModifier = buffer.getInt();
			FormatCode code = buffer.getEnumShort(FormatCode.class);
			
			Type type;
			switch (typeOid) {
			case PgFieldType.INT:
				type = Type.INTEGER;
				break;
			case PgFieldType.VARCHAR:
				type = Type.VARCHAR;
				break;
			default:
				// TODO Convert more typeOids to ADBCJ types
				throw new IllegalStateException("Unable to handle field type with oid " + typeOid);
			}
			
			fields[i] = new PgField(
					resultSet,
					i,
					ioHandler.getConnectionManager().getDatabase(),
					type,
					name,
					tableOid,
					columnAttributeNumber,
					code
					);
		}
		
		RowDescriptionMessage rowDescription = new RowDescriptionMessage(fields);
		out.write(rowDescription);
		out.flush();
	}

}
