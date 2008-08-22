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

// TODO Document ClientCapabilities class
public enum ClientCapabilities {
	LONG_PASSWORD,
	FOUND_ROWS,
	LONG_COLUMN_FLAG,
	CONNECT_WITH_DB,
	NO_SCHEMA,
	COMPRESS,
	ODBC_CLIENT,
	LOCAL_FILES,
	IGNORE_SPACES,
	PROTOCOL_4_1,
	INTERACTIVE,
	SSL,
	IGNORE_SIGPIPE,
	TRANSACTIONS,
	PROTOCOL_4_1_OLD,
	SECURE_AUTHENTICATION,
}
