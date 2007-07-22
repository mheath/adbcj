package edu.byu.cs.adbcj;

import java.util.List;

public interface ResultSet extends List<Row> {

	List<? extends Field> getFields();
	
}
