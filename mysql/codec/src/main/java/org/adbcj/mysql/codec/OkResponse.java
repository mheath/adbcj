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

public class OkResponse extends ServerPacket {
	private final long affectedRows;
	private final long insertId;
	private final Set<ServerStatus> serverStatus;
	private final int warningCount;
	private final String message;

	public OkResponse(int length, int packetNumber, long affectedRows, long insertId, Set<ServerStatus> serverStatus, int warningCount, String message) {
		super(length, packetNumber);
		this.affectedRows = affectedRows;
		this.insertId = insertId;
		this.serverStatus = Collections.unmodifiableSet(serverStatus);
		this.warningCount = warningCount;
		this.message = message;
	}

	public long getAffectedRows() {
		return affectedRows;
	}

	public long getInsertId() {
		return insertId;
	}

	public Set<ServerStatus> getServerStatus() {
		return serverStatus;
	}

	public int getWarningCount() {
		return warningCount;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return String.format("OK response (affected rows: %d, insert id: %d, warning count: %d, message: '%s', server status: %s",
				affectedRows,
				insertId,
				warningCount,
				message,
				serverStatus.toString());
	}

}
