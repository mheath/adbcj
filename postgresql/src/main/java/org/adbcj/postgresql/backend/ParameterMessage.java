package org.adbcj.postgresql.backend;

import org.adbcj.postgresql.ConfigurationVariable;

public class ParameterMessage extends AbstractBackendMessage {

	private final ConfigurationVariable variable;
	private final Object value;
	
	public ParameterMessage(ConfigurationVariable variable, Object value) {
		this.variable = variable;
		this.value = value;
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.PARAMETER_STATUS;
	}

	public Object getValue() {
		return value;
	}
	
	public ConfigurationVariable getVariable() {
		return variable;
	}
	
}
