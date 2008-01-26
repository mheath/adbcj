package org.adbcj.postgresql.backend;

/**
 * Class used for backend messages that don't include any additional information beyond the message type
 * 
 * @author Mike Heath
 */
public final class BackendMessage extends AbstractBackendMessage {

	private final BackendMessageType type;
	
	public BackendMessage(BackendMessageType type) {
		this.type = type;
	}
	
	@Override
	public BackendMessageType getType() {
		return type;
	}

}
