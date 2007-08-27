package edu.byu.cs.adbcj.support;

import java.util.AbstractList;

import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.Row;
import edu.byu.cs.adbcj.Value;

public class DefaultRow extends AbstractList<Value> implements Row {

	private final ResultSet resultSet;
	private final Value[] values;
	
	public DefaultRow(ResultSet resultSet, Value... values) {
		this.resultSet = resultSet;
		this.values = values;
	}
	
	@Override
	public Value get(int index) {
		return values[index];
	}

	@Override
	public int size() {
		return values.length;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}
}
