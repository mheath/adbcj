/*
	This file is part of ADBCJ.

	ADBCJ is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	ADBCJ is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with ADBCJ.  If not, see <http://www.gnu.org/licenses/>.

	Copyright 2008  Mike Heath
*/
package org.adbcj.mysql.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class MySqlClientEncoder {

	private String charset = "ISO8859_1";

	public void encode(ClientRequest request, OutputStream out) throws IOException, NoSuchAlgorithmException {
		int length = request.getLength(charset);

		// Write the length of the packet
		out.write(length & 0xFF);
		out.write(length >> 8 & 0xFF);
		out.write(length >> 16 & 0xFF);


		// Write the packet number
		out.write(request.getPacketNumber());

		if (request instanceof CommandRequest) {
			encodeCommandRequest(out, (CommandRequest)request);
		} else if (request instanceof LoginRequest) {
			encodeLoginRequest(out, (LoginRequest)request);
		} else {
			throw new IllegalStateException("Unable to encode message of type " + request.getClass().getName());
		}

	}

	protected void encodeCommandRequest(OutputStream out, CommandRequest request) throws IOException {
		out.write(request.getCommand().getCommandCode());
		if (request.getPayload() != null && request.getPayload().length() > 0) {
			out.write(request.getPayload().getBytes(charset));
		}
	}

	protected void encodeLoginRequest(OutputStream out, LoginRequest request) throws IOException, NoSuchAlgorithmException {
		// Encode inital part of authentication request
		IoUtils.writeEnumSetShort(out, request.getCapabilities());
		IoUtils.writeEnumSetShort(out, request.getExtendedCapabilities());
		IoUtils.writeInt(out, request.getMaxPacketSize());
		out.write(request.getCharSet().getId());
		out.write(new byte[LoginRequest.FILLER_LENGTH]);

		out.write(request.getCredentials().getUserName().getBytes(charset));
		out.write(0); // null-terminate username

		// Encode password
		final String password = request.getCredentials().getPassword();
		if (password != null && password.length() > 0) {
			byte[] salt = request.getSalt();
			byte[] encryptedPassword = PasswordEncryption.encryptPassword(password, salt);
			out.write(encryptedPassword.length);
			out.write(encryptedPassword);
		} else {
			out.write(0); // null-terminate password
		}

		// Encode desired database/schema
		final String database = request.getCredentials().getDatabase();
		if (database != null) {
			out.write(database.getBytes(charset));
		}
		out.write(0);
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

}
