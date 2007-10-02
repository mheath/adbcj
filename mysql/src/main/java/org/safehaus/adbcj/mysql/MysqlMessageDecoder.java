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

import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.Value;
import org.safehaus.adbcj.support.DefaultRow;
import org.safehaus.adbcj.support.DefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlMessageDecoder extends MessageDecoderAdapter {
	private static final Logger logger = LoggerFactory.getLogger(MysqlMessageDecoder.class);

	private static final byte NULL_VALUE = (byte)0xfb;

	private static final byte RESPONSE_OK = 0x00;
	private static final byte RESPONSE_EOF = (byte)0xfe;
	private static final byte RESPONSE_ERROR = (byte)0xff;

	private static final int GREETING_UNUSED_SIZE = 13;
	private static final int SALT_SIZE = 8;
	private static final int SALT2_SIZE = 12;
	private static final int SQL_STATE_LENGTH = 5;

	private enum State {
		CONNECTING, RESPONSE, FIELD, FIELD_EOF, ROW
	}

	private State state = State.CONNECTING;
	private int fieldPacketCount = 0;
	private int fieldIndex = 0;

	public MessageDecoderResult decodable(IoSession session, ByteBuffer in) {
		in.order(ByteOrder.LITTLE_ENDIAN);

		if (in.remaining() < 3) {
			return NEED_DATA;
		}

		int length = in.getUnsignedMediumInt();
		if (in.remaining() < length + 1) { // TODO Fix + 1 length for binary length encoding
			return NEED_DATA;
		}

		return OK;
	}

	public MessageDecoderResult decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		in.order(ByteOrder.LITTLE_ENDIAN);

		final int packetLength = in.getUnsignedMediumInt();
		final byte packetNumber = in.get();
		final int startPosition = in.position();

		logger.debug("Decoding in state {}", state);
		switch (state) {
		case CONNECTING:
			ServerGreeting serverGreeting = decodeServerGreeting(packetLength, packetNumber, in);
			out.write(serverGreeting);
			state = State.RESPONSE;
			break;
		case RESPONSE:
			int fieldCount = in.get();
			if (fieldCount == RESPONSE_OK) {
				// Create Ok response
				OkResponse okResponse = decodeOkResponse(connection, in, packetLength, packetNumber, startPosition);
				out.write(okResponse);
			} else if (fieldCount == RESPONSE_ERROR) {
				// Create error response
				ErrorResponse response = decodeErrorResponse(connection, in, packetLength, packetNumber, startPosition);
				out.write(response);
			} else if (fieldCount == RESPONSE_EOF) {
				throw new IllegalStateException("Did not expect an EOF response from the server");
			} else {
				// Must be receiving result set header

				// Rewind the buffer to read the binary length encoding
				in.position(in.position() - 1);

				// Get the number of fields. The largest this can be is a 24-bit
				// integer so cast to int is ok
				fieldPacketCount = (int)getBinaryLengthEncoding(in);

				Long extra = null;
				if (in.position() - startPosition - packetLength != 0) {
					extra = getBinaryLengthEncoding(in);
				}

				// Create result set response
				logger.debug("Sending result set response up filter chain");
				ResultSetResponse resultSetResponse = new ResultSetResponse(packetLength, packetNumber,
						fieldPacketCount, extra);
				out.write(resultSetResponse);

				state = State.FIELD;
			}
			break;
		case FIELD:
			ResultSetFieldResponse resultSetFieldResponse = decodeFieldResponse(connection, in, packetLength,
					packetNumber);
			out.write(resultSetFieldResponse);

			fieldPacketCount--;
			if (fieldPacketCount == 0) {
				state = State.FIELD_EOF;
			}
			break;
		case FIELD_EOF:
			EofResponse fieldEof = decodeEofResponse(in, packetLength, packetNumber, EofResponse.Type.FIELD);
			out.write(fieldEof);
			out.flush();

			state = State.ROW;
			fieldIndex = 0;
			break;
		case ROW:
			fieldCount = in.get();
			in.position(in.position() - 1);
			if (fieldCount == RESPONSE_EOF) {
				EofResponse rowEof = decodeEofResponse(in, packetLength, packetNumber, EofResponse.Type.ROW);
				out.write(rowEof);

				state = State.RESPONSE;

				break;
			}

			MysqlResultSet currentResultSet = (MysqlResultSet)connection.getActiveRequest().getPayload();
			fieldCount = currentResultSet.getFieldCount();
			Value[] values = new Value[fieldCount];

			for (int i = 0; i < fieldCount; i++) {
				MysqlField field = currentResultSet.getFields().get(i);
				Object value = null;
				if (in.get() != NULL_VALUE) {
					in.position(in.position() - 1);
					switch (field.getColumnType()) {
					case INTEGER:
					case BIGINT:
						String strVal = decodeLengthCodedString(in, connection.getCharacterSet());
						value = Long.valueOf(strVal);
						break;
					case VARCHAR:
						value = decodeLengthCodedString(in, connection.getCharacterSet());
						break;
					default:
						throw new IllegalStateException("Don't know how to handle column type of "
								+ field.getColumnType());
					}
				}
				values[i] = new DefaultValue(field, value);
			}
			out.write(new ResultSetRowResponse(packetLength, packetNumber, new DefaultRow(currentResultSet, values)));
			break;
		default:
			throw new MysqlException("Unkown decoder state " + state);
		}

		// Sanity check to make sure we're decoding the correct number of bytes
		int diff = (in.position() - startPosition) - packetLength;
		if (diff > 0) {
			throw new DbException(String.format("Read %d too few bytes decoding stream %s", diff, in.getHexDump()));
		}
		if (diff < 0) {
			throw new DbException(String.format("Read %d too many bytes decoding stream %s", -diff, in.getHexDump()));
		}

		return OK;
	}

	private ErrorResponse decodeErrorResponse(MysqlConnection connection, ByteBuffer buffer, final int packetLength,
			final byte packetNumber, final int startPosition) throws CharacterCodingException {
		int errorNumber = buffer.getUnsignedShort();
		buffer.get(); // Throw away sqlstate marker
		String sqlState = buffer.getString(SQL_STATE_LENGTH, MysqlCharacterSet.ASCII_BIN.getCharset().newDecoder());
		String message = buffer.getString(packetLength - (buffer.position() - startPosition), connection
				.getCharacterSet().getCharset().newDecoder());
		ErrorResponse response = new ErrorResponse(packetLength, packetNumber, errorNumber, sqlState, message);
		return response;
	}

	protected ServerGreeting decodeServerGreeting(int length, byte packetNumber, ByteBuffer buffer)
			throws CharacterCodingException {
		byte protocol = buffer.get();
		String version = buffer.getString(MysqlCharacterSet.ASCII_BIN.getCharset().newDecoder());
		int threadId = buffer.getInt();

		byte[] salt = new byte[SALT_SIZE + SALT2_SIZE];
		buffer.get(salt, 0, SALT_SIZE);
		buffer.get(); // Throw away 0 byte

		Set<ClientCapabilities> serverCapabilities = buffer.getEnumSetShort(ClientCapabilities.class);
		MysqlCharacterSet charSet = buffer.getEnum(MysqlCharacterSet.class);
		Set<ServerStatus> serverStatus = buffer.getEnumSetShort(ServerStatus.class);
		buffer.skip(GREETING_UNUSED_SIZE);

		buffer.get(salt, SALT_SIZE, SALT2_SIZE);
		buffer.get(); // Throw away 0 byte

		return new ServerGreeting(length, packetNumber, protocol, version, threadId, salt, serverCapabilities, charSet,
				serverStatus);
	}

	protected OkResponse decodeOkResponse(MysqlConnection connection, ByteBuffer in, final int packetLength,
			final byte packetNumber, final int startPosition) throws CharacterCodingException {
		long affectedRows = getBinaryLengthEncoding(in);
		long insertId = 0;
		if (affectedRows > 0) {
			insertId = getBinaryLengthEncoding(in);
		}
		Set<ServerStatus> serverStatus = in.getEnumSetShort(ServerStatus.class);
		int warningCount = in.getUnsignedShort();
		String message = in.getString(packetLength - (in.position() - startPosition), connection.getCharacterSet()
				.getCharset().newDecoder());

		OkResponse response = new OkResponse(packetLength, packetNumber, affectedRows, insertId, serverStatus,
				warningCount, message);
		return response;
	}

	protected ResultSetFieldResponse decodeFieldResponse(MysqlConnection connection, ByteBuffer buffer,
			int packetLength, byte packetNumber) throws CharacterCodingException {
		String catalogName = decodeLengthCodedString(buffer, connection.getCharacterSet());
		String schemaName = decodeLengthCodedString(buffer, connection.getCharacterSet());
		String tableLabel = decodeLengthCodedString(buffer, connection.getCharacterSet());
		String tableName = decodeLengthCodedString(buffer, connection.getCharacterSet());
		String columnLabel = decodeLengthCodedString(buffer, connection.getCharacterSet());
		String columnName = decodeLengthCodedString(buffer, connection.getCharacterSet());
		buffer.get(); // Skip filler
		int characterSetNumber = buffer.getUnsignedShort();
		MysqlCharacterSet charSet = MysqlCharacterSet.findById(characterSetNumber);
		long length = buffer.getUnsignedInt();
		byte fieldTypeId = buffer.get();
		MysqlType fieldType = MysqlType.findById(fieldTypeId);
		Set<FieldFlag> flags = buffer.getEnumSet(FieldFlag.class);
		int decimals = buffer.getUnsigned();
		buffer.getShort(); // Skip filler
		long fieldDefault = getBinaryLengthEncoding(buffer);
		MysqlField field = new MysqlField(fieldIndex++, catalogName, schemaName, tableLabel, tableName, fieldType, columnLabel,
				columnName, 0, // Figure out precision
				decimals, charSet, length, flags, fieldDefault);
		return new ResultSetFieldResponse(packetLength, packetNumber, field);
	}

	protected EofResponse decodeEofResponse(ByteBuffer in, final int packetLength, final byte packetNumber,
			EofResponse.Type type) {
		// Create EOF response
		byte fieldCount = in.get();

		if (fieldCount != RESPONSE_EOF) {
			throw new MysqlException("Expected an EOF response from the server");
		}

		int warnings = in.getUnsignedShort();
		Set<ServerStatus> serverStatus = in.getEnumSetShort(ServerStatus.class);

		EofResponse response = new EofResponse(packetLength, packetNumber, warnings, serverStatus, type);
		return response;
	}

	private long getBinaryLengthEncoding(ByteBuffer buffer) {
		// This is documented at
		// http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#Elements
		int firstByte = buffer.getUnsigned();
		if (firstByte <= 250) {
			return firstByte;
		}
		if (firstByte == NULL_VALUE) {
			return -1;
		}
		if (firstByte == 252) {
			return buffer.getUnsignedShort();
		}
		if (firstByte == 253) {
			return buffer.getMediumInt();
		}
		if (firstByte == 254) {
			long length = buffer.getLong();
			if (length < 0) {
				throw new DbException("Received length too large to handle");
			}
			return length;
		}
		throw new DbException("Recieved a length value we don't know how to handle");
	}

	private String decodeLengthCodedString(ByteBuffer buffer, MysqlCharacterSet charSet)
			throws CharacterCodingException {
		long length = getBinaryLengthEncoding(buffer);
		if (length > Integer.MAX_VALUE) {
			throw new MysqlException("String too long to decode");
		}
		// TODO Add support to MINA for reading fixed length strings that may contain nulls
		return buffer.getString((int)length, charSet.getCharset().newDecoder());
	}

}
