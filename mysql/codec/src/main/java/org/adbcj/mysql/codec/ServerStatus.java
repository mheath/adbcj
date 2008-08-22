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

/**
 *
 * @author Mike Heath <mheath@apache.org>
 */
// TODO Document ServerStatus and what each bit means
public enum ServerStatus {
	IN_TRANSACTION,
	AUTO_COMMIT,
	MORE_RESULTS,
	MULTI_QUERY,
	BAD_INDEX,
	NO_INDEX,
	CURSOR_EXISTS,
	LAST_ROW_SEBD,
	DATABASE_DROPPED,
	NO_BACKSLASH_ESCAPES
}
