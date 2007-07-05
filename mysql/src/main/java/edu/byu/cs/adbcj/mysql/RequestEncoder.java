package edu.byu.cs.adbcj.mysql;

import java.nio.ByteOrder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

public abstract class RequestEncoder<T extends Request> implements MessageEncoder<T> {

	protected static final int REQUEST_HEADER_SIZE = 4;

	public final void encode(IoSession session, T request, ProtocolEncoderOutput out) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(request.getLength() + REQUEST_HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		int length = request.getLength();
		buffer.put((byte)(length & 0xFF));
		buffer.put((byte)(length >> 8 & 0xFF));
		buffer.put((byte)(length >> 16 & 0xFF));

		buffer.put(request.getPacketNumber());
		
		encode(session, request, buffer);

		buffer.flip();
		out.write(buffer);
	}

	abstract protected void encode(IoSession session, T request, ByteBuffer buffer) throws Exception;
	
}
