package org.adbcj.postgresql.backend;

import java.util.Collections;
import java.util.Map;

import org.adbcj.postgresql.ErrorField;

public class ErrorResponseMessage extends AbstractBackendMessage {

	private final Map<ErrorField, String> fields;
	
	public ErrorResponseMessage(Map<ErrorField, String> fields) {
		this.fields = Collections.unmodifiableMap(fields);
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.ERROR_RESPONSE;
	}
	
	public Map<ErrorField, String> getFields() {
		return fields;
	}

}
