package org.adbcj.postgresql.backend;

public class AuthenticationMessage extends AbstractBackendMessage {

	private final AuthenticationType authenticaitonType;
	private final byte[] data;
	
	public AuthenticationMessage(AuthenticationType authenticationType) {
		this.authenticaitonType = authenticationType;
		data = null;
	}
	
	public AuthenticationMessage(AuthenticationType authenticationType, byte[] data) {
		this.authenticaitonType = authenticationType;
		this.data = data;
	}

	@Override
	public BackendMessageType getType() {
		return BackendMessageType.AUTHENTICATION;
	}

	public AuthenticationType getAuthenticaitonType() {
		return authenticaitonType;
	}
	
	public byte[] getData() {
		return data;
	}
	
}
