package edu.byu.cs.adbcj.mysql;

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
		this.serverStatus = serverStatus;
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
	
}
