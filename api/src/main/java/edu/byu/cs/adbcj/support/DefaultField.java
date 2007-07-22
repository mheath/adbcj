package edu.byu.cs.adbcj.support;

import edu.byu.cs.adbcj.Field;
import edu.byu.cs.adbcj.Type;

public class DefaultField implements Field {

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
}
