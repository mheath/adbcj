package org.safehaus.adbcj.postgresql;

import java.util.Map;

import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbSession;

public class PgException extends DbException {
	private static final long serialVersionUID = 1L;

	private final Map<ErrorField, String> fields;
	
	public PgException(PgConnection connection, Map<ErrorField, String> fields) {
		super(connection);
		this.fields = fields;
	}

	public PgException(PgConnection connection, String message) {
		super(connection, message);
		this.fields = null;
	}

	public PgException(PgConnection connection, String message, Map<ErrorField, String> fields) {
		super(connection, message);
		this.fields = fields;
	}

	public Map<ErrorField, String> getFields() {
		return fields;
	}
	
	@Override
	public PgConnection getSession() {
		return (PgConnection)super.getSession();
	}
	
}
