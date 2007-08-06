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
package edu.byu.cs.adbcj.support;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.Field;
import edu.byu.cs.adbcj.Row;
import edu.byu.cs.adbcj.ResultSet;

public class DefaultResultSet extends AbstractList<Row> implements ResultSet {

	private final int fieldCount;
	private final List<Field> fields;
	
	private final List<Row> results = new ArrayList<Row>();
	
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
	
}
