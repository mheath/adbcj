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


/**
 * Stores the column data type and other meta-data for a particular column. 
 * 
 * @author Mike Heath
 * @see ResultSet
 */
public interface Field {

	/**
	 * The position this column appears in the result set.
	 * 
	 * <p>The value returned by this method is analogous to the value return by
	 * {@link java.sql.ResultSet#findColumn(String)} in JDBC.  However, this value is 0 based in ADBCJ and not 1 based
	 * as it is in JDBC.
	 * 
	 * @return  the position this column appears in the result set, 0 based.
	 */
	int getIndex();
	
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
