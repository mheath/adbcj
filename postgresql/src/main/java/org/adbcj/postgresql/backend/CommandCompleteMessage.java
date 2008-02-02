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
