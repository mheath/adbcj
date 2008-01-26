package org.adbcj.postgresql.backend;

import org.adbcj.postgresql.PgField;

public class RowDescriptionMessage extends AbstractBackendMessage {

	private final PgField[] fields;
	
	RowDescriptionMessage(PgField[] fields) {
		this.fields = fields;
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.ROW_DESCRIPTION;
	}
	
	public PgField[] getFields() {
		return fields;
	}

}
