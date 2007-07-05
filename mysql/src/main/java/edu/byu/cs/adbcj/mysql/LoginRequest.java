package edu.byu.cs.adbcj.mysql;

import java.util.Set;

import org.apache.mina.common.ByteBuffer;

public class LoginRequest extends Request {

	public static final int MAX_PACKET_SIZE = 0x00ffffff;
	
	public static final int FILLER_LENGTH = 23;
	public static final int PASSWORD_LENGTH = 20;

	private final LoginCredentials credentials;
	private final Set<ClientCapabilities> capabilities;
	private final Set<ExtendedClientCapabilities> extendedCapabilities;
	
	public LoginRequest(LoginCredentials credentials, Set<ClientCapabilities> capabilities, Set<ExtendedClientCapabilities> extendedCapabilities) {
		this.credentials = credentials;
		this.capabilities = capabilities;
		this.extendedCapabilities = extendedCapabilities;
	}
	
	@Override
	int getLength() {
		return 2 // Client Capabilities field
				+ 2 // Extended Client Capabilities field
				+ 4 // Max packet size field
				+ 1 // Char set
				+ FILLER_LENGTH
				+ credentials.getUserName().length() + 1
				+ PASSWORD_LENGTH
				+ 1 // Filler after password
				+ credentials.getDatabase().length() + 1;
	}
	
	@Override
	public byte getPacketNumber() {
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
		return MAX_PACKET_SIZE; // TODO: Make this configurable
	}

	public MysqlCharacterSet getCharSet() {
		return MysqlCharacterSet.LATIN1_SWEDISH_CI; // TODO: Make this configurable
	}
	
}
