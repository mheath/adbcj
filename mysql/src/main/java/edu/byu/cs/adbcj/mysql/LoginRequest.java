package edu.byu.cs.adbcj.mysql;

import java.nio.charset.CharacterCodingException;
import java.util.Set;

public class LoginRequest extends Request {

	public static final int MAX_PACKET_SIZE = 0x00ffffff;
	
	public static final int FILLER_LENGTH = 23;
	public static final int PASSWORD_LENGTH = 20;

	private final LoginCredentials credentials;
	private final Set<ClientCapabilities> capabilities;
	private final Set<ExtendedClientCapabilities> extendedCapabilities;
	private final MysqlCharacterSet charset;
	
	public LoginRequest(LoginCredentials credentials, Set<ClientCapabilities> capabilities, Set<ExtendedClientCapabilities> extendedCapabilities, MysqlCharacterSet charset) {
		this.credentials = credentials;
		this.capabilities = capabilities;
		this.extendedCapabilities = extendedCapabilities;
		this.charset = charset;
	}
	
	@Override
	int getLength(MysqlCharacterSet charset) throws CharacterCodingException {
		return 2 // Client Capabilities field
				+ 2 // Extended Client Capabilities field
				+ 4 // Max packet size field
				+ 1 // Char set
				+ FILLER_LENGTH
				+ charset.encodedLength(credentials.getUserName()) + 1
				+ PASSWORD_LENGTH
				+ 1 // Filler after password
				+ charset.encodedLength(credentials.getDatabase()) + 1;
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
		return charset;
	}
	
}
