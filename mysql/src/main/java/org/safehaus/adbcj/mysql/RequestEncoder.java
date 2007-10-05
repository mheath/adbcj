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

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

public abstract class RequestEncoder<T extends MysqlRequest> implements MessageEncoder<T> {

	protected static final int REQUEST_HEADER_SIZE = 4;

	public final void encode(IoSession session, T request, ProtocolEncoderOutput out) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		int length = request.getLength(connection.getServerGreeting().getCharacterSet());

		IoBuffer buffer = IoBuffer.allocate(length + REQUEST_HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.put((byte)(length & 0xFF));
		buffer.put((byte)(length >> 8 & 0xFF));
		buffer.put((byte)(length >> 16 & 0xFF));

		buffer.put(request.getPacketNumber());
		
		encode(session, request, buffer);

		buffer.flip();
		out.write(buffer);
	}

	abstract protected void encode(IoSession session, T request, IoBuffer buffer) throws Exception;
	
}
