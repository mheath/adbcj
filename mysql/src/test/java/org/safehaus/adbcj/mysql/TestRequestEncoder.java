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

import java.nio.charset.CharacterCodingException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.DummySession;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.safehaus.adbcj.mysql.IoSessionUtil;
import org.safehaus.adbcj.mysql.MysqlCharacterSet;
import org.safehaus.adbcj.mysql.MysqlConnection;
import org.safehaus.adbcj.mysql.MysqlRequest;
import org.safehaus.adbcj.mysql.RequestEncoder;
import org.safehaus.adbcj.mysql.ServerGreeting;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRequestEncoder {

	@Test
	public void testHeaderEncoding() throws Exception {
		final int length = 0xabcdef;
		final int packetNumber = (byte)0xff;
		final AtomicBoolean invokedEncode = new AtomicBoolean(false);
		final AtomicBoolean invokedWrite = new AtomicBoolean(false);

		final MysqlRequest myRequest = new MysqlRequest() {
			@Override
			int getLength(MysqlCharacterSet charset) {
				return length;
			}
			@Override
			public byte getPacketNumber() {
				return packetNumber;
			}
		};
		
		RequestEncoder<MysqlRequest> encoder = new RequestEncoder<MysqlRequest>() {
			@Override
			protected void encode(IoSession session, MysqlRequest request, IoBuffer buffer) throws CharacterCodingException {
				invokedEncode.set(true);

				Assert.assertSame(myRequest, request);
				Assert.assertEquals(length, request.getLength(MysqlCharacterSet.LATIN1_SWEDISH_CI));
				Assert.assertEquals(length + REQUEST_HEADER_SIZE, buffer.capacity());
				
				Assert.assertNotNull(buffer);
			}
			public Set<Class<? extends MysqlRequest>> getMessageTypes() {
				return null;
			}
		};

		// TODO Create utility method for putting dummy connection in a dummy IoSession
		IoSession ioSession = new DummySession();
		MysqlConnection connection = new MysqlConnection(null, null, null);
		connection.setServerGreeting(new ServerGreeting(0, (byte)0, (byte)0, "", 0, null, null, null, null));
		IoSessionUtil.setMysqlConnection(ioSession, connection);
		encoder.encode(ioSession, myRequest, new ProtocolEncoderOutput() {
			public WriteFuture flush() {
				return null;
			}

			public void mergeAll() {
			}

			public void write(IoBuffer buf) {
				invokedWrite.set(true);
				Assert.assertEquals((byte)0xef, buf.get());
				Assert.assertEquals((byte)0xcd, buf.get());
				Assert.assertEquals((byte)0xab, buf.get());
				
				Assert.assertEquals(packetNumber, buf.get());
			}
		});
		
		Assert.assertTrue(invokedEncode.get(), "The RequestEncoder.encode(IoSession, Request, ByteBuffer) method was never called");
		Assert.assertTrue(invokedWrite.get(), "The ProtocolEncoderOutput.write(ByteBuffer) method was never called");
	}

}
