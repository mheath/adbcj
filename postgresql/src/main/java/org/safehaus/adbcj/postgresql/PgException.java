package org.safehaus.adbcj.postgresql;

import java.util.Map;

import org.safehaus.adbcj.DbException;

public class PgException extends DbException {
	private static final long serialVersionUID = 1L;

	private final Map<ErrorField, String> fields;
	
	public PgException(Map<ErrorField, String> fields) {
		super();
		this.fields = fields;
	}

	public PgException(String message, Map<ErrorField, String> fields) {
		super(message);
		this.fields = fields;
	}

	public Map<ErrorField, String> getFields() {
		return fields;
	}
	
}
