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
package edu.byu.cs.adbcj.mysql;

import java.nio.charset.CharacterCodingException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Test;

public class TestRequestEncoder {

	@Test
	public void testHeaderEncoding() throws Exception {
		final int length = 0xabcdef;
		final int packetNumber = (byte)0xff;
		final AtomicBoolean invokedEncode = new AtomicBoolean(false);
		final AtomicBoolean invokedWrite = new AtomicBoolean(false);

		final Request myRequest = new Request() {
			@Override
			int getLength(MysqlCharacterSet charset) {
				return length;
			}
			@Override
			public byte getPacketNumber() {
				return packetNumber;
			}
		};
		
		RequestEncoder<Request> encoder = new RequestEncoder<Request>() {
			@Override
			protected void encode(IoSession session, Request request, ByteBuffer buffer) throws CharacterCodingException {
				invokedEncode.set(true);

				Assert.assertSame(myRequest, request);
				Assert.assertEquals(length, request.getLength(MysqlCharacterSet.LATIN1_SWEDISH_CI));
				Assert.assertEquals(length + REQUEST_HEADER_SIZE, buffer.capacity());
				
				Assert.assertNotNull(buffer);
			}
			public Set<Class<Request>> getMessageTypes() {
				return null;
			}
		};

		encoder.encode(null, myRequest, new ProtocolEncoderOutput() {
			public WriteFuture flush() {
				return null;
			}

			public void mergeAll() {
			}

			public void write(ByteBuffer buf) {
				invokedWrite.set(true);
				Assert.assertEquals((byte)0xef, buf.get());
				Assert.assertEquals((byte)0xcd, buf.get());
				Assert.assertEquals((byte)0xab, buf.get());
				
				Assert.assertEquals(packetNumber, buf.get());
			}
		});
		
		Assert.assertTrue("The RequestEncoder.encode(IoSession, Request, ByteBuffer) method was never called", invokedEncode.get());
		Assert.assertTrue("The ProtocolEncoderOutput.write(ByteBuffer) method was never called", invokedWrite.get());
	}

}
