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

import java.io.InputStream;

import org.adbcj.mysql.codec.MySqlClientDecoder;
import org.adbcj.mysql.codec.ServerPacket;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlMessageDecoder extends CumulativeProtocolDecoder {
	private static final Logger logger = LoggerFactory.getLogger(MysqlMessageDecoder.class);

	private final MySqlClientDecoder decoder = new MySqlClientDecoder();

	@Override
	protected boolean doDecode(IoSession session, IoBuffer buffer, ProtocolDecoderOutput out) throws Exception {
		logger.trace("Decoding message");
		InputStream in = buffer.asInputStream();
		try {
			ServerPacket message = decoder.decode(in, false);
			if (message == null) {
				return false;
			}
			out.write(message);
			out.flush();
			return buffer.hasRemaining();
		} finally {
			in.close();
		}
	}

}
