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
package org.adbcj.mysql.mina;

import java.io.OutputStream;

import org.adbcj.mysql.codec.ClientRequest;
import org.adbcj.mysql.codec.MySqlClientEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MysqlMessageEncoder implements ProtocolEncoder {

	protected static final int REQUEST_HEADER_SIZE = 4;

	private final MySqlClientEncoder encoder = new MySqlClientEncoder();

	@Override
	public void dispose(IoSession session) throws Exception {
		// Nothing to dispose
	}

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput encoderOut) throws Exception {
		IoBuffer buffer = IoBuffer.allocate(1024);
		OutputStream out = buffer.asOutputStream();
		try {
			encoder.encode((ClientRequest) message, out);
		} finally {
			out.close();
		}
		buffer.flip();
		encoderOut.write(buffer);
	}

}
