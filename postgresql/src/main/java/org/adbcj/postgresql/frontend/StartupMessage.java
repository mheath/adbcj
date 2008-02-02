/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.postgresql.frontend;

import java.util.Collections;
import java.util.Map;

import org.adbcj.postgresql.ConfigurationVariable;

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
