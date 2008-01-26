package org.adbcj.postgresql.backend;

public class CommandCompleteMessage extends AbstractBackendMessage {

	private final Command command;
	private final long rowCount;
	private final int oid;

	public CommandCompleteMessage(Command command, long rowCount, int oid) {
		this.command = command;
		this.rowCount = rowCount;
		this.oid = oid;
	}

	@Override
	public BackendMessageType getType() {
		return BackendMessageType.COMMAND_COMPLETE;
	}

	public Command getCommand() {
		return command;
	}

	public long getRowCount() {
		return rowCount;
	}

	public int getOid() {
		return oid;
	}

}
