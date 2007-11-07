package org.safehaus.adbcj.postgresql.backend;

import org.safehaus.adbcj.Value;

public class DataRowMessage extends AbstractBackendMessage{

	private final Value[] values;
	
	public DataRowMessage(Value[] values) {
		this.values = values;
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.DATA_ROW;
	}
	
	public Value[] getValues() {
		return values;
	}
	
}
