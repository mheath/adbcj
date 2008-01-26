package org.adbcj.postgresql.frontend;

public class CloseMessage extends AbstractFrontendMessage {

	private final byte target;
	private final String name;

	public static CloseMessage createCloseStatementMessage(String name) {
		return new CloseMessage((byte)'S', name);
	}
	
	public static CloseMessage createClosePortalMessage(String name) {
		return new CloseMessage((byte)'P', name);
	}
	
	private CloseMessage(byte target, String name) {
		this.target = target;
		this.name = name;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.CLOSE;
	}

	public String getName() {
		return name;
	}

	public byte getTarget() {
		return target;
	}

}
