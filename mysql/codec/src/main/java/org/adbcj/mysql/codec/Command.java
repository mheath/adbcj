/*
	This file is part of ADBCJ.

	ADBCJ is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	ADBCJ is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with ADBCJ.  If not, see <http://www.gnu.org/licenses/>.

	Copyright 2008  Mike Heath
*/
package org.adbcj.mysql.codec;

public enum Command {
	SLEEP(0x00),
	QUIT(0x01),
	INIT_DB(0x02),
	QUERY(0x03),
	FIELD_LIST(0x04),
	CREATE_DB(0x05),
	DROP_DB(0x06),
	REFRESH(0x07),
	SHUTDOWN(0x08),
	STATISTICS(0x09),
	PROCESS_INFO(0x0a),
	CONNECT(0x0b),
	PROCESS_KILL(0x0c),
	DEBUG(0x0c),
	PING(0x0d),
	TIME(0x0e),
	DELAYED_INSERT(0x0f),
	CHANGED_USER(0x10),
	BINLOG_DUMP(0x11),
	TABLE_DUMP(0x12),
	CONNECT_OUT(0x13),
	REGISTER_SLAVE(0x14),
	STATEMENT_PREPARE(0x14),
	STATEMENT_EXECUTE(0x15),
	STATEMENT_SEND_LONG_DATA(0x16),
	STATEMENT_CLOSE(0x17),
	STATEMENT_RESET(0x18),
	SET_OPTION(0x19),
	STATEMENT_FETCH(0x1a);

	private int commandCode;

	Command(int commandCode) {
		this.commandCode = commandCode;
	}

	public int getCommandCode() {
		return commandCode;
	}
}
