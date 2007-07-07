package edu.byu.cs.adbcj.mysql;

import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

import edu.byu.cs.adbcj.DbException;

public class MysqlMessageDecoder extends MessageDecoderAdapter {

	public static final Charset CHARSET = Charset.forName("US-ASCII");
	private static final int SALT_SIZE = 8;
	private static final int GREETING_UNUSED_SIZE = 13;
	private static final int SALT2_SIZE = 12;
	
	private static int getPacketLength(ByteBuffer buffer) {
		int b1 = buffer.get();
		int b2 = buffer.get();
		int b3 = buffer.get();
		
		return b3 << 16 | b2 << 8 | b1;
	}
	
	private static long getBinaryLengthEncoding(ByteBuffer buffer) {
		// This is documented at http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#Elements
		int firstByte = buffer.getUnsigned();
		if (firstByte <= 250) {
			return firstByte;
		}
		if (firstByte == 251) {
			return -1;
		}
		if (firstByte == 252) {
			return buffer.getUnsignedShort();
		}
		if (firstByte == 253) {
			return buffer.getUnsignedInt();
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
	
	public MessageDecoderResult decodable(IoSession session, ByteBuffer in) {
		int length = getPacketLength(in);
		if (in.remaining() < length + 1) {
			return NEED_DATA;
		}
		
		return OK;
	}

	public MessageDecoderResult decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		in.order(ByteOrder.LITTLE_ENDIAN);
		
		final int length = getPacketLength(in);
		final byte packetNumber = in.get();
		final int startPosition = in.position();

		// If we have written anything, the server is sending its greeting
		if (session.getWrittenBytes() == 0) {
			ServerGreeting serverGreeting = decodeServerGreeting(length, packetNumber, in);
			out.write(serverGreeting);
			return OK;
		}
		
		byte fieldCount = in.get();
		if (fieldCount == 0x00) {
			// Create Ok response
			long affectedRows = getBinaryLengthEncoding(in);
			long insertId = 0;
			if (affectedRows > 0) {
				insertId = getBinaryLengthEncoding(in);
			}
			Set<ServerStatus> serverStatus = in.getEnumSetShort(ServerStatus.class);
			int warningCount = in.getUnsignedShort();
			String message = in.getString(length - (in.position() - startPosition), connection.getCharacterSet().getCharset().newDecoder());
			
			OkResponse response = new OkResponse(length, packetNumber, affectedRows, insertId, serverStatus, warningCount, message);
			out.write(response);
		} else if (fieldCount == 0xff) {
			// Create error response
		} else if (fieldCount == 0xfe) {
			// Create EOF response
		} else {
			// Create result set response
		}
		
		return OK;
	}

	private ServerGreeting decodeServerGreeting(int length, byte packetNumber, ByteBuffer buffer) throws CharacterCodingException {
		byte protocol = buffer.get();
		String version = buffer.getString(CHARSET.newDecoder());
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
		
		return new ServerGreeting(length, packetNumber, protocol, version, threadId, salt, serverCapabilities, charSet, serverStatus);
	}

}
