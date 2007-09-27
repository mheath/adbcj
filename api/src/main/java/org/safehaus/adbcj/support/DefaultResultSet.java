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
package org.safehaus.adbcj.support;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.Field;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Row;


public class DefaultResultSet extends AbstractList<Row> implements ResultSet {

	private final int fieldCount;
	private final List<Field> fields;
	
	private final List<Row> results = new ArrayList<Row>();
	
	private final Map<Object, Field> fieldMapping = new HashMap<Object, Field>();
	
	public DefaultResultSet(int fieldCount) {
		this.fieldCount = fieldCount;
		fields = new ArrayList<Field>(fieldCount);
	}
	
	@Override
	public Row get(int index) {
		return results.get(index);
	}

	@Override
	public int size() {
		return results.size();
	}

	public List<? extends Field> getFields() {
		return Collections.unmodifiableList(fields);
	}

	public void addField(Field field) {
		if (fields.size() >= fieldCount) {
			throw new DbException("Field count exceeded");
		}
		fields.add(field);
	}
	
	public int getFieldCount() {
		return fieldCount;
	}
	
	public boolean addResult(Row result) {
		return results.add(result);
	}
	
	public Field getField(Object key) {
		if (key == null) {
			return null;
		}
		if (key instanceof Field) {
			return (Field)key;
		}
		Field field = fieldMapping.get(key);
		if (field != null) {
			return field;
		}
		if (key instanceof Number) {
			int index = ((Number)key).intValue();
			field = fields.get(index);
			fieldMapping.put(key, field);
			return field;
		}
		String stringKey = key.toString();
		if (stringKey == null) {
			return null;
		}
		// Search by column label
		for (Field f : fields) {
			if (stringKey.equals(f.getColumnLabel())) {
				fieldMapping.put(key, f);
				return f;
			}
		}
		// Search by column name
		for (Field f : fields) {
			if (stringKey.equals(f.getColumnName())) {
				fieldMapping.put(key, f);
				return f;
			}
		}
		// Search by table label.column label
		for (Field f : fields) {
			if (stringKey.equals(f.getTableLabel() + "." + f.getColumnLabel())) {
				fieldMapping.put(key, f);
				return f;
			}
		}
		// Search by table name.column name
		for (Field f : fields) {
			if (stringKey.equals(f.getTableName() + "." + f.getColumnName())) {
				fieldMapping.put(key, f);
				return f;
			}
		}
		return null;
	}
}
