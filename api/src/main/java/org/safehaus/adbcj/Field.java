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
package org.safehaus.adbcj;

public interface Field {

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
