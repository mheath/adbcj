package edu.byu.cs.adbcj.mysql;

import java.util.Collections;
import java.util.Set;

class ServerGreeting extends Response {

	private final byte protocol;
	private final String version;
	private final int threadId;
	private final byte[] salt;
	private final Set<ClientCapabilities> serverCapabilities;
	private final MysqlCharacterSet characterSet;
	private final Set<ServerStatus> serverStatus;
	
	public ServerGreeting (int length, byte packetNumber, byte protocol, String version, int threadId, byte[] salt, Set<ClientCapabilities> serverCapabilities, MysqlCharacterSet characterSet, Set<ServerStatus> serverStatus) {
		super(length, packetNumber);
		this.protocol = protocol;
		this.version = version;
		this.threadId = threadId;
		this.salt = salt;
		this.serverCapabilities = serverCapabilities;
		this.characterSet = characterSet;
		this.serverStatus = serverStatus;
	}
	
	public byte getProtocol() {
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
