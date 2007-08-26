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

import java.util.Collections;
import java.util.Set;

public class OkResponse extends Response {
	private final long affectedRows;
	private final long insertId;
	private final Set<ServerStatus> serverStatus;
	private final int warningCount;
	private final String message;

	public OkResponse(int length, byte packetNumber, long affectedRows, long insertId, Set<ServerStatus> serverStatus, int warningCount, String message) {
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
