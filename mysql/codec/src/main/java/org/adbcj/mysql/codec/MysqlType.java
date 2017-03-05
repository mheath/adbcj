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
    
	SIGNED_DECIMAL(MysqlDefs.FIELD_TYPE_DECIMAL, Type.DECIMAL),   
	SIGNED_NEW_DECIMAL(MysqlDefs.FIELD_TYPE_NEW_DECIMAL,Type.DECIMAL),
	SIGNED_TINY(MysqlDefs.FIELD_TYPE_TINY, Type.TINYINT),
	SIGNED_SHORT(MysqlDefs.FIELD_TYPE_SHORT, Type.SMALLINT),
	SIGNED_LONG(MysqlDefs.FIELD_TYPE_LONG, Type.INTEGER),
	SIGNED_FLOAT(MysqlDefs.FIELD_TYPE_FLOAT, Type.FLOAT),
	SIGNED_DOUBLE(MysqlDefs.FIELD_TYPE_DOUBLE, Type.DOUBLE),
	SIGNED_NULL(MysqlDefs.FIELD_TYPE_NULL, Type.NULL),
	SIGNED_TIMESTAMP(MysqlDefs.FIELD_TYPE_TIMESTAMP, Type.TIMESTAMP),
	SIGNED_LONGLONG(MysqlDefs.FIELD_TYPE_LONGLONG, Type.LONG),
	SIGNED_INT24(MysqlDefs.FIELD_TYPE_INT24, Type.INTEGER),
	SIGNED_DATE(MysqlDefs.FIELD_TYPE_DATE, Type.DATE),
	SIGNED_TIME(MysqlDefs.FIELD_TYPE_TIME, Type.TIME),
	SIGNED_DATETIME(MysqlDefs.FIELD_TYPE_DATETIME, Type.TIMESTAMP),
	SIGNED_YEAR(MysqlDefs.FIELD_TYPE_YEAR, Type.INTEGER),
	SIGNED_NEWDATE(MysqlDefs.FIELD_TYPE_NEWDATE, Type.DATE),
	SIGNED_VARCHAR(MysqlDefs.FIELD_TYPE_VARCHAR, Type.VARCHAR),
	SIGNED_BIT(MysqlDefs.FIELD_TYPE_BIT, Type.BIT),
	SIGNED_NEWDECIMAL(MysqlDefs.FIELD_TYPE_NEW_DECIMAL, Type.DECIMAL),
	SIGNED_ENUM(MysqlDefs.FIELD_TYPE_ENUM, Type.INTEGER),
	SIGNED_SET(MysqlDefs.FIELD_TYPE_SET, Type.ARRAY),
	SIGNED_TINY_BLOB(MysqlDefs.FIELD_TYPE_TINY_BLOB, Type.BLOB),
	SIGNED_MEDIUM_BLOB(MysqlDefs.FIELD_TYPE_MEDIUM_BLOB, Type.BLOB),
	SIGNED_LONG_BLOB(MysqlDefs.FIELD_TYPE_LONG_BLOB, Type.BLOB),
	SIGNED_BLOB(MysqlDefs.FIELD_TYPE_BLOB, Type.BLOB),
	SIGNED_VAR_STRING(MysqlDefs.FIELD_TYPE_VAR_STRING, Type.VARCHAR),
	SIGNED_STRING(MysqlDefs.FIELD_TYPE_STRING, Type.VARCHAR),
	SIGNED_GEOMETRY(MysqlDefs.FIELD_TYPE_GEOMETRY, Type.STRUCT),
	UNSIGNED_LONGLONG(MysqlDefs.FIELD_TYPE_LONGLONG,Type.BIGINT),
	UNSIGNED_LONG(MysqlDefs.FIELD_TYPE_LONG,Type.LONG);

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

	public static MysqlType findById(int id,boolean unsign) {
		for (MysqlType type : values()) {
			if (id == type.id) {
			    if(unsign)
			    {
			       if(type == SIGNED_LONG)
			       {
			           return UNSIGNED_LONG;
			       }
			       else if(type == SIGNED_LONGLONG)
			       {
			           return UNSIGNED_LONGLONG;
			       }
			    }
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
