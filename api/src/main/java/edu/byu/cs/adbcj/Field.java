package edu.byu.cs.adbcj;

public interface Field {

	String getCatalogName();
	
	String getFieldClassName();
	
	String getColumnLabel();
	
	String getColumnName();
	
	Type getColumnType();
	
	int getPrecision();
	
	int getScale();
	
	String getSchemaName();
	
	String getTableLabel();
	
	String getTableName();
	
	boolean isAutoIncrement();
	
	boolean isCaseSensitive();
	
	boolean isCurrency();
	
	boolean isDefinitelyWritable();
	
	boolean isNullable();
	
	boolean isReadOnly();
	
	boolean isSearchable();
	
	boolean isSigned();
	
	boolean isWritable();
	
}
