package org.adbcj;

import java.math.BigDecimal;
import java.util.Date;

public interface Value {

	Field getField();
	
	BigDecimal getBigDecimal();
	
	boolean getBoolean();
	
	Date getDate();
	
	double getDouble();
	
	float getFloat();
	
	int getInt();
	
	long getLong();
	
	String getString();
	
	Object getValue();

	boolean isNull();
	
}
