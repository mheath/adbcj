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

import java.util.Collections;
import java.util.Set;

public class ServerGreeting extends ServerPacket {
	private final int protocol;
	private final String version;
	private final int threadId;
	private final byte[] salt;
	private final Set<ClientCapabilities> serverCapabilities;
	private final MysqlCharacterSet characterSet;
	private final Set<ServerStatus> serverStatus;

	public ServerGreeting (int length, int packetNumber, int protocol, String version, int threadId, byte[] salt, Set<ClientCapabilities> serverCapabilities, MysqlCharacterSet characterSet, Set<ServerStatus> serverStatus) {
		super(length, packetNumber);
		this.protocol = protocol;
		this.version = version;
		this.threadId = threadId;
		this.salt = salt;
		this.serverCapabilities = serverCapabilities;
		this.characterSet = characterSet;
		this.serverStatus = serverStatus;
	}

	public int getProtocol() {
		return protocol;
	}

	public String getVersion() {
		return version;
	}

	public int getThreadId() {
		return threadId;
	}

	public byte[] getSalt() {
		return salt;
	}

	public Set<ClientCapabilities> getServerCapabilities() {
		return Collections.unmodifiableSet(serverCapabilities);
	}

	public MysqlCharacterSet getCharacterSet() {
		return characterSet;
	}

	public Set<ServerStatus> getServerStatus() {
		return serverStatus;
	}

}
