package org.adbcj.postgresql.frontend;

public class CancelRequestMessage extends AbstractFrontendMessage {

	private final int processId;
	private final int secretKey;
	
	public CancelRequestMessage(int processId, int secretKey) {
		this.processId = processId;
		this.secretKey = secretKey;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.CANCEL_REQUEST;
	}

	public int getProcessId() {
		return processId;
	}

	public int getSecretKey() {
		return secretKey;
	}

}
