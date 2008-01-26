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

enum Command {
	SLEEP((byte)0x00),
	QUIT((byte)0x01),
	INIT_DB((byte)0x02),
	QUERY((byte)0x03),
	FIELD_LIST((byte)0x04),
	CREATE_DB((byte)0x05),
	DROP_DB((byte)0x06),
	REFRESH((byte)0x07),
	SHUTDOWN((byte)0x08),
	STATISTICS((byte)0x09),
	PROCESS_INFO((byte)0x0a),
	CONNECT((byte)0x0b),
	PROCESS_KILL((byte)0x0c),
	DEBUG((byte)0x0c),
	PING((byte)0x0d),
	TIME((byte)0x0e),
	DELAYED_INSERT((byte)0x0f),
	CHANGED_USER((byte)0x10),
	BINLOG_DUMP((byte)0x11),
	TABLE_DUMP((byte)0x12),
	CONNECT_OUT((byte)0x13),
	REGISTER_SLAVE((byte)0x14),
	STATEMENT_PREPARE((byte)0x14),
	STATEMENT_EXECUTE((byte)0x15),
	STATEMENT_SEND_LONG_DATA((byte)0x16),
	STATEMENT_CLOSE((byte)0x17),
	STATEMENT_RESET((byte)0x18),
	SET_OPTION((byte)0x19),
	STATEMENT_FETCH((byte)0x1a);
	
	private byte commandCode;
	
	Command(byte commandCode) {
		this.commandCode = commandCode;
	}
	
	public byte getCommandCode() {
		return commandCode;
	}
}
