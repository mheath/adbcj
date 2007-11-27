package org.safehaus.adbcj.postgresql;

import org.safehaus.adbcj.Field;
import org.safehaus.adbcj.Type;

// TODO Add support for fetching extended Postgresql meta-data so we can have all the same info the MySQL has - do this on a ConnectionManager basis
// TODO Add support for meta-data caching to ADBCJ API to facilitate invalidating cache on live systems
public class PgField implements Field {

	private final int index;
	private final String schemaName;
	private final Type type;
	private final String columnLabel;
	private final int tableOid;
	private final int columnAttributeNumber;
	private final FormatCode formatCode;
	private final int typeSize;
	private final int typeModifier;
	
	public PgField(int index, String schemaName, Type type, String columnLabel, int tableOid, int columnAttributeNumber, FormatCode formatCode, int typeSize, int typeModifier) {
		this.index = index;
		this.schemaName = schemaName;
		this.type = type;
		this.columnLabel = columnLabel;
		this.tableOid = tableOid;
		this.columnAttributeNumber = columnAttributeNumber;
		this.formatCode = formatCode;
		this.typeSize = typeSize;
		this.typeModifier = typeModifier;
	}
	
	public FormatCode getFormatCode() {
		return formatCode;
	}
	
	public String getCatalogName() {
		// TODO Figure out pg catalog name
		return null;
	}

	public String getColumnLabel() {
		// TODO Add some way of looking up cached meta-data
		return columnLabel;
	}

	public String getColumnName() {
		// TODO Add some way of looking up cached meta-data
		return null;
	}

	public Type getColumnType() {
		return type;
	}

	public String getFieldClassName() {
		// TODO Figure out the field class name
		return null;
	}

	public int getIndex() {
		return index;
	}

	public int getPrecision() {
		// TODO Figure out how to get precision
		return 0;
	}

	public int getScale() {
		// TODO Figure out how to get scale
		return 0;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableLabel() {
		// TODO Can we even get the table label from Postgres?
		return getTableName();
	}

	public String getTableName() {
		// TODO: Add some way of looking up cached meta-data
		return null;
	}

	public boolean isAutoIncrement() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isCaseSensitive() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isCurrency() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isDefinitelyWritable() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isNullable() {
		// TODO: Add some way of looking up cached meta-data
		return false;
	}

	public boolean isReadOnly() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isSearchable() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isSigned() {
		// TODO Can we get this from Postgres?
		return false;
	}

	public boolean isWritable() {
		// TODO Can we get this from Postgres?
		return false;
	}

}
