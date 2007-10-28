package org.safehaus.adbcj.postgresql.frontend;

import java.util.Collections;
import java.util.Map;

import org.safehaus.adbcj.postgresql.ConfigurationVariable;

public class StartupMessage extends AbstractFrontendMessage {

	/**
	 * Protocol version.  First two bytes are major version; last two bytes are minor version.
	 */
	public static final byte[] PROTOCOL_VERSION = {0x00, 0x03, 0x00, 0x00};

	private final String username;
	private final String database;
	private final Map<ConfigurationVariable, String> parameters;
	
	public StartupMessage(String username, String database, Map<ConfigurationVariable, String> parameters) {
		this.username = username;
		this.database = database;
		this.parameters = Collections.unmodifiableMap(parameters);
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getDatabase() {
		return database;
	}
	
	public Map<ConfigurationVariable, String> getParameters() {
		return parameters;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.STARTUP;
	}
}
