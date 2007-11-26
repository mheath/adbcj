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

import org.apache.mina.common.IoBuffer;
import org.apache.mina.common.IoSession;

public class LoginRequestEncoder extends RequestEncoder<LoginRequest> {

	@Override
	protected void encode(IoSession session, LoginRequest request, IoBuffer buffer) throws Exception {
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
