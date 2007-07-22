package edu.byu.cs.adbcj.mysql;

import java.util.Set;

public class EofResponse extends Response {

	public enum Type {
		FIELD,
		ROW
	}
	
	private final int warnings;
	private final Set<ServerStatus> serverStatus;
	private final Type type;
	
	public EofResponse(int packetLength, byte packetNumber, int warnings, Set<ServerStatus> serverStatus, Type type) {
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
