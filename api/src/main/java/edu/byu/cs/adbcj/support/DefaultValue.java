package edu.byu.cs.adbcj.support;

import java.math.BigDecimal;
import java.util.Date;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.Field;
import edu.byu.cs.adbcj.Value;

public class DefaultValue implements Value {

	private final Field field;
	private final Object value;

	public DefaultValue(Field field, Object value) {
		this.field = field;
		this.value = value;
	}

	public Field getField() {
		return field;
	}

	public BigDecimal getBigDecimal() {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal)value;
		}
		return new BigDecimal(value.toString());
	}

	public boolean getBoolean() {
		if (value == null) {
			return false;
		}
		if (value instanceof Boolean) {
			return ((Boolean)value).booleanValue();
		}
		return Boolean.valueOf(value.toString());
	}

	public Date getDate() {
		if (value == null) {
			return null;
		}
		if (value instanceof Date) {
			return (Date)value;
		}
		throw new DbException(String.format("%s is not a date", value.toString()));
	}

	public double getDouble() {
		if (value == null) {
			return 0d;
		}
		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}
		return Double.valueOf(value.toString());
	}

	public float getFloat() {
		if (value == null) {
			return 0f;
		}
		if (value instanceof Number) {
			return ((Number)value).floatValue();
		}
		return Float.valueOf(value.toString());
	}

	public int getInt() {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number) {
			return ((Number)value).intValue();
		}
		return Integer.valueOf(value.toString());
	}

	public long getLong() {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number) {
			return ((Number)value).longValue();
		}
		return Long.valueOf(value.toString());
	}

	public String getString() {
		return value == null ? null : value.toString();
	}

	public Object getValue() {
		return value;
	}

	public boolean isNull() {
		return value == null;
	}

}
