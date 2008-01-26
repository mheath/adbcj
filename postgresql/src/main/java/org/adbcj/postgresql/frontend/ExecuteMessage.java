package org.adbcj.postgresql.frontend;

public class ExecuteMessage extends AbstractFrontendMessage {

	private final String portal;
	private final int maxRows;
	
	public ExecuteMessage() {
		this(null, 0);
	}
	
	public ExecuteMessage(int maxRows) {
		this(null, maxRows);
	}
	
	public ExecuteMessage(String portal, int maxRows) {
		this.portal = portal;
		this.maxRows = maxRows;
	}

	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.EXECUTE;
	}

	public String getPortal() {
		return portal;
	}

	public int getMaxRows() {
		return maxRows;
	}

}
