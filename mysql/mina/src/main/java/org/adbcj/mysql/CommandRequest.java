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
package org.adbcj.mysql;

import java.nio.charset.CharacterCodingException;

public class CommandRequest extends MysqlRequest {

	private final Command command;
	private final String payload;
	
	public CommandRequest(Command command) {
		this.command = command;
		this.payload = null;
	}
	
	public CommandRequest(Command command, String payload) {
		this.command = command;
		this.payload = payload;
	}
	
	public Command getCommand() {
		return command;
	}
	
	public String getPayload() {
		return payload;
	}
	
	@Override
	int getLength(MysqlCharacterSet charset) throws CharacterCodingException {
		return 1 + ((payload == null) ? 0 : charset.encodedLength(payload));
	}
	
}
