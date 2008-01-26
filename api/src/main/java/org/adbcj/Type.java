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
package org.adbcj;

import java.sql.Types;

public enum Type {
	ARRAY(Types.ARRAY),
	BIGINT(Types.BIGINT),
	BINARY(Types.BINARY),
	BIT(Types.BIT),
	BLOB(Types.BLOB),
	BOOLEAN(Types.BOOLEAN),
	CHAR(Types.CHAR),
	CLOB(Types.CLOB),
	DATALINK(Types.DATALINK),
	DATE(Types.DATE),
	DECIMAL(Types.DECIMAL),
	DISTINCT(Types.DISTINCT),
	DOUBLE(Types.DOUBLE),
	FLOAT(Types.FLOAT),
	INTEGER(Types.INTEGER),
	JAVA_OBJECT(Types.JAVA_OBJECT),
	LONGVARBINARY(Types.LONGVARBINARY),
	LONGVARCHAR(Types.LONGVARCHAR),
	NULL(Types.NULL),
	NUMERIC(Types.NUMERIC),
	OTHER(Types.OTHER),
	REAL(Types.REAL),
	REF(Types.REF),
	SMALLINT(Types.SMALLINT),
	STRUCT(Types.STRUCT),
	TIME(Types.TIME),
	TIMESTAMP(Types.TIMESTAMP),
	TINYINT(Types.TINYINT),
	VARBINARY(Types.VARBINARY),
	VARCHAR(Types.VARCHAR);

	private final int jdbcType;

	private Type(int jdbcType) {
		this.jdbcType = jdbcType;
	}

	public int getJdbcType() {
		return jdbcType;
	}

	public static Type fromJdbcType(int jdbcType) {
		switch (jdbcType) {
		case Types.ARRAY:
			return Type.ARRAY;
		case Types.BIGINT:
			return Type.BIGINT;
		case Types.BINARY:
			return Type.BINARY;
		case Types.BIT:
			return Type.BIT;
		case Types.BLOB:
			return Type.BLOB;
		case Types.BOOLEAN:
			return Type.BOOLEAN;
		case Types.CHAR:
			return Type.CHAR;
		case Types.CLOB:
			return Type.CLOB;
		case Types.DATALINK:
			return Type.DATALINK;
		case Types.DATE:
			return Type.DATE;
		case Types.DECIMAL:
			return Type.DECIMAL;
		case Types.DISTINCT:
			return Type.DISTINCT;
		case Types.DOUBLE:
			return Type.DOUBLE;
		case Types.FLOAT:
			return Type.FLOAT;
		case Types.INTEGER:
			return Type.INTEGER;
		case Types.JAVA_OBJECT:
			return Type.JAVA_OBJECT;
		case Types.LONGNVARCHAR:
			return Type.LONGVARCHAR;
		case Types.LONGVARBINARY:
			return Type.LONGVARBINARY;
		case Types.LONGVARCHAR:
			return Type.LONGVARCHAR;
		case Types.NULL:
			return Type.NULL;
		case Types.NUMERIC:
			return Type.NUMERIC;
		case Types.OTHER:
			return Type.OTHER;
		case Types.REAL:
			return Type.REAL;
		case Types.REF:
			return Type.REF;
		case Types.SMALLINT:
			return Type.SMALLINT;
		case Types.STRUCT:
			return Type.STRUCT;
		case Types.TIME:
			return Type.TIME;
		case Types.TIMESTAMP:
			return Type.TIMESTAMP;
		case Types.TINYINT:
			return Type.TINYINT;
		case Types.VARBINARY:
			return Type.VARBINARY;
		case Types.VARCHAR:
			return Type.VARCHAR;
		default:
			throw new IllegalStateException("Don't know how to handle column type" + jdbcType);
		}
	}
}
