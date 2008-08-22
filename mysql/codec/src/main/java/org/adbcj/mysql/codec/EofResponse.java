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

import java.util.Set;

public class EofResponse extends ServerPacket {

	public enum Type {
		FIELD,
		ROW
	}

	private final int warnings;
	private final Set<ServerStatus> serverStatus;
	private final Type type;

	public EofResponse(int packetLength, int packetNumber, int warnings, Set<ServerStatus> serverStatus, Type type) {
		super(packetLength, packetNumber);
		this.warnings = warnings;
		this.serverStatus = serverStatus;
		this.type = type;
	}

	public int getWarnings() {
		return warnings;
	}

	public Set<ServerStatus> getServerStatus() {
		return serverStatus;
	}

	public Type getType() {
		return type;
	}

}
