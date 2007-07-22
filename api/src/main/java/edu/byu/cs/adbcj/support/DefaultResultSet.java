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
