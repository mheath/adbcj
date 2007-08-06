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
package edu.byu.cs.adbcj.mysql;

import edu.byu.cs.adbcj.Type;

// TODO Make sure all the types are mapped up properly - Most of these are guesses
public enum MysqlType {
	DECIMAL((byte)0x00, Type.DECIMAL),
	TINY((byte)0x01, Type.TINYINT),
	SHORT((byte)0x02, Type.SMALLINT),
	LONG((byte)0x03, Type.BIGINT),
	FLOAT((byte)0x04, Type.FLOAT),
	DOUBLE((byte)0x05, Type.DOUBLE),
	NULL((byte)0x06, Type.NULL),
	TIMESTAMP((byte)0x07, Type.TIMESTAMP),
	LONGLONG((byte)0x08, Type.BIGINT),
	INT24((byte)0x09, Type.INTEGER),
	DATE((byte)0x0a, Type.DATE),
	TIME((byte)0x0b, Type.TIME),
	DATETIME((byte)0x0c, Type.TIMESTAMP),
	YEAR((byte)0x0d, Type.INTEGER),
	NEWDATE((byte)0x0e, Type.DATE),
	VARCHAR((byte)0x0f, Type.VARCHAR),
	BIT((byte)0x10, Type.BIT),
	NEWDECIMAL((byte)0xf6, Type.DECIMAL),
	ENUM((byte)0xf7, Type.INTEGER),
	SET((byte)0xf8, Type.ARRAY),
	TINY_BLOB((byte)0xf9, Type.BLOB),
	MEDIUM_BLOB((byte)0xfa, Type.BLOB),
	LONG_BLOB((byte)0xfb, Type.BLOB),
	BLOB((byte)0xfc, Type.BLOB),
	VAR_STRING((byte)0xfd, Type.VARCHAR),
	STRING((byte)0xfe, Type.VARCHAR),
	GEOMETRY((byte)0xff, Type.STRUCT);

	private final byte id;
	private final Type type;
	
	MysqlType(byte id, Type type) {
		this.id = id;
		this.type = type;
	}
	
	public byte getId() {
		return id;
	}
	
	public Type getType() {
		return type;
	}
	
	public static MysqlType findById(byte id) {
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
		return ""; // TODO Figure out
	}
}
