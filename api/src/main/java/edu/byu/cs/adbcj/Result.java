package edu.byu.cs.adbcj;

import java.util.List;

public interface Result {

	Long getAffectedRows();

	List<String> getWarnings();
	
	DbFuture<ResultSet> getGeneratedKeys();
	
}
