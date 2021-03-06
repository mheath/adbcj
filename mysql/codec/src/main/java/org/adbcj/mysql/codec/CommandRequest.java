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

import java.io.UnsupportedEncodingException;


public class CommandRequest extends ClientRequest {

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
	public int getLength(String charset) throws UnsupportedEncodingException {
		return 1 + ((payload == null) ? 0 : payload.getBytes(charset).length);
	}

}
