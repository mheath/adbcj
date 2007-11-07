package org.safehaus.adbcj.postgresql.frontend;

public class DescribeMessage extends AbstractFrontendMessage {
	private final byte target;
	private final String name;

	public static DescribeMessage createDescribeStatementMessage(String name) {
		return new DescribeMessage((byte)'S', name);
	}
	
	public static DescribeMessage createDescribePortalMessage(String name) {
		return new DescribeMessage((byte)'P', name);
	}
	
	private DescribeMessage(byte target, String name) {
		this.target = target;
		this.name = name;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.DESCRIBE;
	}

	public String getName() {
		return name;
	}

	public byte getTarget() {
		return target;
	}

}
