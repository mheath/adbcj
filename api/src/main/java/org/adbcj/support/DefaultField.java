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
package org.adbcj.support;

import org.adbcj.Field;
import org.adbcj.Type;

public class DefaultField implements Field {

	private final int index;
	private final String catalogName;
	private final String schemaName;
	private final String tableLabel;
	private final String tableName;
	private final Type columnType;
	private final String columnLabel;
	private final String columnName;
	private final int precision;
	private final int scale;
	private final boolean autoIncrement;
	private final boolean caseSensitive;
	private final boolean currency;
	private final boolean definitelyWritable;
	private final boolean nullable;
	private final boolean readOnly;
	private final boolean searchable;
	private final boolean signed;
	private final boolean writable;
	private final String fieldClassName;
	
	public DefaultField(
			int index,
			String catalogName,
			String schemaName,
			String tableLabel,
			String tableName,
			Type columnType,
			String columnLabel,
			String columnName,
			int precision,
			int scale,
			boolean autoIncrement,
			boolean caseSensitive,
			boolean currency,
			boolean definitelyWritable,
			boolean nullable,
			boolean readOnly,
			boolean searchable,
			boolean signed,
			boolean writable,
			String fieldClassName
			) {
		this.index = index;
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.tableLabel = tableLabel;
		this.tableName = tableName;
		this.columnType = columnType;
		this.columnLabel = columnLabel;
		this.columnName = columnName;
		this.precision = precision;
		this.scale = scale;
		this.autoIncrement = autoIncrement;
		this.caseSensitive = caseSensitive;
		this.currency = currency;
		this.definitelyWritable = definitelyWritable;
		this.nullable = nullable;
		this.readOnly = readOnly;
		this.searchable = searchable;
		this.signed = signed;
		this.writable = writable;
		this.fieldClassName = fieldClassName;
	}
	
	public int getIndex() {
		return index;
	}
	
	public String getCatalogName() {
		return catalogName;
	}

	public String getColumnLabel() {
		return columnLabel;
	}

	public String getColumnName() {
		return columnName;
	}

	public Type getColumnType() {
		return columnType;
	}

	public String getFieldClassName() {
		return fieldClassName;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableLabel() {
		return tableLabel;
	}

	public String getTableName() {
		return tableName;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public boolean isCurrency() {
		return currency;
	}

	public boolean isDefinitelyWritable() {
		return definitelyWritable;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isSearchable() {
		return searchable;
	}

	public boolean isSigned() {
		return signed;
	}

	public boolean isWritable() {
		return writable;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DefaultField other = (DefaultField)obj;
		return index == other.index;
	}
	
}
