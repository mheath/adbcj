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

import java.io.UnsupportedEncodingException;
import java.util.Set;


public class LoginRequest extends ClientRequest {

	public static final int MAX_PACKET_SIZE = 0x00ffffff;

	public static final int FILLER_LENGTH = 23;
	public static final int PASSWORD_LENGTH = 20;

	private final LoginCredentials credentials;
	private final Set<ClientCapabilities> capabilities;
	private final Set<ExtendedClientCapabilities> extendedCapabilities;
	private final MysqlCharacterSet charset;

	private final byte[] salt;

	public LoginRequest(LoginCredentials credentials, Set<ClientCapabilities> capabilities, Set<ExtendedClientCapabilities> extendedCapabilities, MysqlCharacterSet charset, byte[] salt) {
		this.credentials = credentials;
		this.capabilities = capabilities;
		this.extendedCapabilities = extendedCapabilities;
		this.charset = charset;
		this.salt = salt.clone();
	}

	@Override
	public int getLength(String charset) throws UnsupportedEncodingException {
		return 2 // Client Capabilities field
				+ 2 // Extended Client Capabilities field
				+ 4 // Max packet size field
				+ 1 // Char set
				+ FILLER_LENGTH
				+ credentials.getUserName().getBytes(charset).length + 1
				+ ((credentials.getPassword() == null || credentials.getPassword().length() == 0) ? 0 : PASSWORD_LENGTH)
				+ 1 // Filler after password
				+ credentials.getDatabase().getBytes(charset).length + 1;
	}

	@Override
	public int getPacketNumber() {
		return 1;
	}

	public Set<ClientCapabilities> getCapabilities() {
		return capabilities;
	}

	public Set<ExtendedClientCapabilities> getExtendedCapabilities() {
		return extendedCapabilities;
	}

	public LoginCredentials getCredentials() {
		return credentials;
	}

	public int getMaxPacketSize() {
		return MAX_PACKET_SIZE; // TODO Make MySQL max packet size configurable
	}

	public MysqlCharacterSet getCharSet() {
		return charset;
	}

	public byte[] getSalt() {
		return salt.clone();
	}

}
