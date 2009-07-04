/*
	This file is part of ADBCJ.

	ADBCJ is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	ADBCJ is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with ADBCJ.  If not, see <http://www.gnu.org/licenses/>.

	Copyright 2008  Mike Heath
 */
package org.adbcj.mysql.codec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.adbcj.Value;
import org.adbcj.support.DefaultValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client stateful decoder.  Being stateful, each client connection must have its own decoder instance to function
 * properly.
 *
 * @author Mike Heath <mheath@apache.org>
 *
 */
public class MySqlClientDecoder {
	private static final Logger logger = LoggerFactory.getLogger(MySqlClientDecoder.class);

	/**
	 * The salt size in a server greeting
	 */
	public static final int SALT_SIZE = 8;

	/**
	 * The size of the second salt in a server greeting
	 */
	public static final int SALT2_SIZE = 12;

	/**
	 * Number of unused bytes in server greeting
	 */
	public static final int GREETING_UNUSED_SIZE = 13;

	public static final int SQL_STATE_LENGTH = 5;

	public static final int RESPONSE_OK = 0x00;
	public static final int RESPONSE_EOF = 0xfe;
	public static final int RESPONSE_ERROR = 0xff;
	/**
	 * The state of the decoder.
	 */
	enum State {
		/**
		 * The client is connecting
		 */
		CONNECTING,
		RESPONSE,
		FIELD,
		FIELD_EOF,
		ROW
	}

	private State state = State.CONNECTING;

	private String charset = "ISO8859_1";

	/**
	 * Holds the remaining number of field packets expected to build the result set
	 */
	private int expectedFieldPackets = 0;

	private int fieldIndex = 0;

	/**
	 * The field definitions for the current result set
	 */
	private MysqlField[] fields;

	/**
	 * Decodes a message from a MySql server.
	 *
	 * @param input  the {@code InputStream} from which to decode the message
	 * @param block  true if the decoder can block, false otherwise
	 * @return  the decode message, null if the {@code block} is {@code} false and there is not enough data available
	 * 				to decode the message without blocking
	 * @throws IOException  thrown if an error occurs reading data from the inputstream
	 */
	public ServerPacket decode(InputStream input, boolean block) throws IOException {
		// If mark is not support and we can't block, throw an exception
		if (!input.markSupported() && !block) {
			throw new IllegalArgumentException("Non-blocking decoding requires an InputStream that supports marking");
		}
		// TODO This should be the max packet size - make this configurable
		input.mark(Integer.MAX_VALUE);
		ServerPacket message = null;
		try {
			message = doDecode(input, block);
		} finally {
			if (message == null) {
				input.reset();
			}
		}
		return message;
	}

	protected ServerPacket doDecode(InputStream input, boolean block) throws IOException {
		// If we can't block, make sure there's enough data available to read
		if (!block) {
			if (input.available() < 3) {
				return null;
			}
		}
		// Read the packet length
		final int length = IoUtils.readUnsignedMediumInt(input);

		// If we can't block, make sure the stream has enough data
		if (!block) {
			// Make sure we have enough data for the packet length and the packet number
			if (input.available() < length + 1) {
				return null;
			}
		}
		final int packetNumber = IoUtils.safeRead(input);
		BoundedInputStream in = new BoundedInputStream(input, length);
		boolean threwException = false;
		try {

			logger.trace("Decoding in state {}", state);
			switch (state) {
			case CONNECTING:
				ServerGreeting serverGreeting = decodeServerGreeting(in, length, packetNumber);
				state = State.RESPONSE;
				return serverGreeting;
			case RESPONSE:
				int fieldCount = in.read();
				if (fieldCount == RESPONSE_OK) {
					// Create Ok response
					return decodeOkResponse(in, length, packetNumber);
				}
				if (fieldCount == RESPONSE_ERROR) {
					// Create error response
					ErrorResponse response = decodeErrorResponse(in, length, packetNumber);
					return response;
				}
				if (fieldCount == RESPONSE_EOF) {
					throw new IllegalStateException("Did not expect an EOF response from the server");
				}
				// Must be receiving result set header

				// Get the number of fields. The largest this can be is a 24-bit
				// integer so cast to int is ok
				expectedFieldPackets = (int)IoUtils.readBinaryLengthEncoding(in, fieldCount);
				fields = new MysqlField[expectedFieldPackets];
				logger.trace("Field count {}", expectedFieldPackets);

				Long extra = null;
				if (in.getRemaining() > 0) {
					extra = IoUtils.readBinaryLengthEncoding(in);
				}

				state = State.FIELD;

				return new ResultSetResponse(length, packetNumber, expectedFieldPackets, extra);
			case FIELD:
				ResultSetFieldResponse resultSetFieldResponse = decodeFieldResponse(in, length, packetNumber);

				expectedFieldPackets--;
				logger.trace("fieldPacketCount: {}", expectedFieldPackets);
				if (expectedFieldPackets == 0) {
					state = State.FIELD_EOF;
				}
				return resultSetFieldResponse;
			case FIELD_EOF:
				fieldCount = in.read();

				if (fieldCount != RESPONSE_EOF) {
					throw new IllegalStateException("Expected an EOF response from the server");
				}
				EofResponse fieldEof = decodeEofResponse(in, length, packetNumber, EofResponse.Type.FIELD);
				state = State.ROW;
				fieldIndex = 0;
				return fieldEof;
			case ROW:
				fieldCount = in.read(); // This is only for checking for EOF
				if (fieldCount == RESPONSE_EOF) {
					EofResponse rowEof = decodeEofResponse(in, length, packetNumber, EofResponse.Type.ROW);

					state = State.RESPONSE;

					return rowEof;
				}

				Value[] values = new Value[fields.length];
				for (int i = 0; i < fields.length; ) {
					MysqlField field = fields[i++];
					Object value = null;
					if (fieldCount != IoUtils.NULL_VALUE) {
						// We will have to move this as some datatypes will not be sent across the wire as strings
						String strVal = IoUtils.readLengthCodedString(in, fieldCount, charset);

						// TODO add decoding for all column types
						switch (field.getColumnType()) {
						case TINYINT:
							value = Byte.valueOf(strVal);
							break;
						case INTEGER:
						case BIGINT:
							value = Long.valueOf(strVal);
							break;
						case VARCHAR:
							value = strVal;
							break;
						default:
							throw new IllegalStateException("Don't know how to handle column type of "
									+ field.getColumnType());
						}
					}
					values[field.getIndex()] = new DefaultValue(field, value);
					if (i < fields.length) {
						fieldCount = in.read();
					}

				}
				return new ResultSetRowResponse(length, packetNumber, values);
			default:
				throw new IllegalStateException("Unkown decoder state " + state);
			}
		} catch (IOException e) {
			threwException = true;
			throw e;
		} catch (RuntimeException e) {
			threwException = true;
			throw e;
		} finally {
			if (!threwException && in.getRemaining() > 0) {
				throw new IllegalStateException("Buffer underrun occured; remaining bytes: " + in.getRemaining());
			}
		}
	}

	protected ServerGreeting decodeServerGreeting(InputStream in, int length, int packetNumber) throws IOException {
		int protocol = IoUtils.safeRead(in);
		String version = IoUtils.readString(in, "ASCII");
		int threadId = IoUtils.readInt(in);

		byte[] salt = new byte[SALT_SIZE + SALT2_SIZE];
		in.read(salt, 0, SALT_SIZE);
		in.read(); // Throw away 0 byte

		Set<ClientCapabilities> serverCapabilities = IoUtils.readEnumSetShort(in, ClientCapabilities.class);
		MysqlCharacterSet charSet = MysqlCharacterSet.findById(in.read());
		Set<ServerStatus> serverStatus = IoUtils.readEnumSetShort(in, ServerStatus.class);
		in.skip(GREETING_UNUSED_SIZE);

		in.read(salt, SALT_SIZE, SALT2_SIZE);
		in.read(); // Throw away 0 byte

		return new ServerGreeting(length, packetNumber, protocol, version, threadId, salt, serverCapabilities, charSet,
				serverStatus);
	}

	protected OkResponse decodeOkResponse(BoundedInputStream in, int length, int packetNumber) throws IOException {
		long affectedRows = IoUtils.readBinaryLengthEncoding(in);
		long insertId = IoUtils.readBinaryLengthEncoding(in);
		Set<ServerStatus> serverStatus = IoUtils.readEnumSetShort(in, ServerStatus.class);
		int warningCount = IoUtils.readUnsignedShort(in);
		String message = IoUtils.readFixedLengthString(in, in.getRemaining(), charset);

		return new OkResponse(length, packetNumber, affectedRows, insertId, serverStatus,
				warningCount, message);
	}

	protected ErrorResponse decodeErrorResponse(InputStream in, int length, int packetNumber) throws IOException {
		int errorNumber = IoUtils.readUnsignedShort(in);
		in.read(); // Throw away sqlstate marker
		String sqlState = IoUtils.readString(in, "ASCII");
		String message = IoUtils.readString(in, charset);
		return new ErrorResponse(length, packetNumber, errorNumber, sqlState, message);
	}

	protected EofResponse decodeEofResponse(InputStream in, int length, int packetNumber, EofResponse.Type type) throws IOException {
		int warnings = IoUtils.readUnsignedShort(in);
		Set<ServerStatus> serverStatus = IoUtils.readEnumSetShort(in, ServerStatus.class);

		return new EofResponse(length, packetNumber, warnings, serverStatus, type);
	}

	protected ResultSetFieldResponse decodeFieldResponse(InputStream in,
			int packetLength, int packetNumber) throws IOException {
		String catalogName = IoUtils.readLengthCodedString(in, charset);
		String schemaName = IoUtils.readLengthCodedString(in, charset);
		String tableLabel = IoUtils.readLengthCodedString(in, charset);
		String tableName = IoUtils.readLengthCodedString(in, charset);
		String columnLabel = IoUtils.readLengthCodedString(in, charset);
		String columnName = IoUtils.readLengthCodedString(in, charset);
		in.read(); // Skip filler
		int characterSetNumber = IoUtils.readUnsignedShort(in);
		MysqlCharacterSet charSet = MysqlCharacterSet.findById(characterSetNumber);
		long length = IoUtils.readUnsignedInt(in);
		int fieldTypeId = in.read();
		MysqlType fieldType = MysqlType.findById(fieldTypeId);
		Set<FieldFlag> flags = IoUtils.readEnumSet(in, FieldFlag.class);
		int decimals = in.read();
		in.skip(2); // Skip filler
		long fieldDefault = IoUtils.readBinaryLengthEncoding(in);
		MysqlField field = new MysqlField(fieldIndex, catalogName, schemaName, tableLabel, tableName, fieldType, columnLabel,
				columnName, 0, // Figure out precision
				decimals, charSet, length, flags, fieldDefault);
		fields[fieldIndex++] = field;
		return new ResultSetFieldResponse(packetLength, packetNumber, field);
	}

	// TODO: This stream implementation doesn't even work b ecause it doesn't delegate all InputStream methods
	private static class BoundedInputStream extends InputStream {

		private final InputStream in;
		private int remaining;

		public BoundedInputStream(InputStream in, int length) {
			this.in = in;
			this.remaining = length;
		}

		@Override
		public int read() throws IOException {
			int i = in.read();
			if (i >= 0) {
				remaining --;
			}
			if (remaining < 0) {
				throw new IllegalStateException("Buffer overrun");
			}
			return i;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int i = in.read(b, off, len);
			remaining -= i;
			if (remaining < 0) {
				throw new IllegalStateException("Read too many bytes");
			}
			return i;
		}

		@Override
		public long skip(long n) throws IOException {
			long i = in.skip(n);
			remaining -= i;
			if (remaining < 0) {
				throw new IllegalStateException("Read too many bytes");
			}
			return i;
		}

		public int getRemaining() {
			return remaining;
		}

	}

	/**
	 * Sets the state, used for testing.
	 *
	 * @param state
	 */
	void setState(State state) {
		this.state = state;
	}
}
