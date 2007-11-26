package org.safehaus.adbcj.support;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.safehaus.adbcj.Field;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Row;
import org.safehaus.adbcj.Value;


public class DefaultRow extends AbstractMap<Object, Value> implements Row {

	private final ResultSet resultSet;
	private final Value[] values;
	
	private transient Set<java.util.Map.Entry<Object, Value>> entrySet;
	
	public DefaultRow(ResultSet resultSet, Value... values) {
		this.resultSet = resultSet;
		this.values = values;
	}

	@Override
	public Set<java.util.Map.Entry<Object, Value>> entrySet() {
		if (entrySet == null) {
			Set<java.util.Map.Entry<Object, Value>> set = new HashSet<Entry<Object,Value>>();
			for (Value value : values) {
				set.add(new AbstractMap.SimpleEntry<Object, Value>(value.getField(), value));
			}
			entrySet = Collections.unmodifiableSet(set);
		}
		return entrySet;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}
	
	@Override
	public int size() {
		return values.length;
	}
	
	@Override
	public boolean containsKey(Object key) {
		return resultSet.getField(key) != null;
	}
	
	@Override
	public Value get(Object key) {
		Field field = resultSet.getField(key);
		return values[field.getIndex()];
	}
	
	@Override
	public Value remove(Object key) {
		throw new UnsupportedOperationException("Results set rows are read-only");
	}
	
	@Override
	public void clear() {
		throw new UnsupportedOperationException("Results set rows are read-only");
	}
	
	public Value[] getValues() {
		return values;
	}
}
