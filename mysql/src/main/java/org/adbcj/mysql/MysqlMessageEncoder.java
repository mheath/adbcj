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
package org.adbcj.mysql;

import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.security.NoSuchAlgorithmException;

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class MysqlMessageEncoder implements ProtocolEncoder {

	protected static final int REQUEST_HEADER_SIZE = 4;

	public void dispose(IoSession session) throws Exception {
		// Nothing to dispose
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		MysqlRequest request = (MysqlRequest)message;
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		int length = request.getLength(connection.getServerGreeting().getCharacterSet());

		IoBuffer buffer = IoBuffer.allocate(length + REQUEST_HEADER_SIZE);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.put((byte)(length & 0xFF));
		buffer.put((byte)(length >> 8 & 0xFF));
		buffer.put((byte)(length >> 16 & 0xFF));

		buffer.put(request.getPacketNumber());
		
		if (message instanceof CommandRequest) {
			encodeCommandRequest(connection, session, (CommandRequest)request, buffer);
		} else if (message instanceof LoginRequest) {
			encodeLoginRequest(session, (LoginRequest)message, buffer);
		} else {
			throw new IllegalStateException("Unable to encode message of type " + request.getClass().getName());
		}

		buffer.flip();
		out.write(buffer);
	}

	protected void encodeCommandRequest(MysqlConnection connection, IoSession session, CommandRequest request, IoBuffer buffer) throws CharacterCodingException {
		buffer.put(request.getCommand().getCommandCode());
		if (request.getPayload() != null && request.getPayload().length() > 0) {
			buffer.putString(request.getPayload(), connection.getCharacterSet().getCharset().newEncoder());
		}
	}

	protected void encodeLoginRequest(IoSession session, LoginRequest request, IoBuffer buffer) throws CharacterCodingException, NoSuchAlgorithmException {
		// Encode inital part of authentication request
		buffer.putEnumSetShort(request.getCapabilities());
		buffer.putEnumSetShort(request.getExtendedCapabilities());
		buffer.putInt(request.getMaxPacketSize());
		buffer.put(request.getCharSet().getId());
		
		// Encode filler
		buffer.fill((byte)0, LoginRequest.FILLER_LENGTH);
		
		// Encode username
		buffer.putString(request.getCredentials().getUserName(), request.getCharSet().getCharset().newEncoder());
		buffer.put((byte)0); // null-terminate username
		
		// Encode password
		final String password = request.getCredentials().getPassword();
		if (password != null && password.length() > 0) {
			byte[] salt = IoSessionUtil.getMysqlConnection(session).getServerGreeting().getSalt();
			byte[] encryptedPassword = PasswordEncryption.encryptPassword(password, salt);
			buffer.put((byte)encryptedPassword.length);
			buffer.put(encryptedPassword);
		} else {
			buffer.put((byte)0); // null-terminate password
		}
		
		// Encode desired database/schema
		final String database = request.getCredentials().getDatabase();
		if (database != null) {
			buffer.putString(database, request.getCharSet().getCharset().newEncoder());
		}
		buffer.put((byte)0);
	}

}
