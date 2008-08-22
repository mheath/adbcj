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

import org.adbcj.Type;

// TODO Make sure all the types are mapped up properly - Most of these are guesses
public enum MysqlType {
	DECIMAL(0x00, Type.DECIMAL),
	TINY(0x01, Type.TINYINT),
	SHORT(0x02, Type.SMALLINT),
	LONG(0x03, Type.BIGINT),
	FLOAT(0x04, Type.FLOAT),
	DOUBLE(0x05, Type.DOUBLE),
	NULL(0x06, Type.NULL),
	TIMESTAMP(0x07, Type.TIMESTAMP),
	LONGLONG(0x08, Type.BIGINT),
	INT24(0x09, Type.INTEGER),
	DATE(0x0a, Type.DATE),
	TIME(0x0b, Type.TIME),
	DATETIME(0x0c, Type.TIMESTAMP),
	YEAR(0x0d, Type.INTEGER),
	NEWDATE(0x0e, Type.DATE),
	VARCHAR(0x0f, Type.VARCHAR),
	BIT(0x10, Type.BIT),
	NEWDECIMAL(0xf6, Type.DECIMAL),
	ENUM(0xf7, Type.INTEGER),
	SET(0xf8, Type.ARRAY),
	TINY_BLOB(0xf9, Type.BLOB),
	MEDIUM_BLOB(0xfa, Type.BLOB),
	LONG_BLOB(0xfb, Type.BLOB),
	BLOB(0xfc, Type.BLOB),
	VAR_STRING(0xfd, Type.VARCHAR),
	STRING(0xfe, Type.VARCHAR),
	GEOMETRY(0xff, Type.STRUCT);

	private final int id;
	private final Type type;

	MysqlType(int id, Type type) {
		this.id = id;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public static MysqlType findById(int id) {
		for (MysqlType type : values()) {
			if (id == type.id) {
				return type;
			}
		}
		return null;
	}

	// TODO We need to reverse-engineer all these values

	public boolean isCaseSensitive() {
		return false;
	}

	public boolean isCurrency() {
		return false;
	}

	public String getClassName() {
		return ""; // TODO Figure out what should be returned for a MySQL type class name
	}
}
