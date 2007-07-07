package edu.byu.cs.adbcj.mysql;

import java.util.Collections;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;

public class CommandRequestEncoder extends RequestEncoder<CommandRequest> {

	@Override
	protected void encode(IoSession session, CommandRequest request, ByteBuffer buffer)	throws Exception {
		buffer.put(request.getCommand().getCommandCode());
	}

	public Set<Class<CommandRequest>> getMessageTypes() {
		return Collections.singleton(CommandRequest.class);
	}

}
