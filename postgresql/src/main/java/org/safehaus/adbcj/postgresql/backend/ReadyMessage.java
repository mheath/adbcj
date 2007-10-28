package org.safehaus.adbcj.postgresql.backend;

public class ReadyMessage extends AbstractBackendMessage {

	private final Status status;
	
	public ReadyMessage(Status status) {
		this.status = status;
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.READY_FOR_QUERY;
	}
	
	public Status getStatus() {
		return status;
	}

}
