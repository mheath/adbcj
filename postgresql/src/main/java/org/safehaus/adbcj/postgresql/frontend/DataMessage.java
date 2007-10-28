package org.safehaus.adbcj.postgresql.frontend;

public class DataMessage extends AbstractFrontendMessage {

	private final FrontendMessageType type;
	private final byte[] data;

	public DataMessage(FrontendMessageType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public FrontendMessageType getType() {
		return type;
	}
	
	public byte[] getData() {
		return data;
	}
	
}
