package org.adbcj.postgresql.backend;

public class KeyMessage extends AbstractBackendMessage {

	private final int pid;
	private final int key;
	
	public KeyMessage(int pid, int key) {
		this.pid = pid;
		this.key = key;
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.KEY;
	}

	public int getPid() {
		return pid;
	}

	public int getKey() {
		return key;
	}

}
