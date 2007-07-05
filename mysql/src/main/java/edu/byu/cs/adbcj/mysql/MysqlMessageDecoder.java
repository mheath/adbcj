package edu.byu.cs.adbcj.mysql;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

public class MysqlMessageDecoder extends MessageDecoderAdapter {

	public static final Charset CHARSET = Charset.forName("US-ASCII");
	private static final int SALT_SIZE = 8;
	private static final int GREETING_UNUSED_SIZE = 13;
	private static final int SALT2_SIZE = 12;
	
	private int getPacketLength(ByteBuffer buffer) {
		int b1 = buffer.get();
		int b2 = buffer.get();
		int b3 = buffer.get();
		
		return b3 << 16 | b2 << 8 | b1;
	}
	
	public MessageDecoderResult decodable(IoSession session, ByteBuffer in) {
		int length = getPacketLength(in);
		if (in.remaining() < length + 1) {
			return NEED_DATA;
		}
		
		return OK;
	}

	public MessageDecoderResult decode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
		int length = getPacketLength(in);
		byte packetNumber = in.get();
		
		State state = (State)session.getAttribute(SessionId.STATE.toString(), State.CONNECTING);
		
		switch (state) {
		case CONNECTING:
			ServerGreeting serverGreeting = decodeServerGreeting(in);
			out.write(serverGreeting);
			break;
		case CONNECTED:
			throw new IllegalStateException("Implement me");
		}
		
		return OK;
	}

	private ServerGreeting decodeServerGreeting(ByteBuffer buffer) throws CharacterCodingException {
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
		
		return new ServerGreeting(protocol, version, threadId, salt, serverCapabilities, charSet, serverStatus);
	}

}
