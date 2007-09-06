package edu.byu.cs.adbcj.support;

import java.util.List;

import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.ResultSet;

public class DefaultResult implements Result {

	final Long affectedRows;
	final List<String> warnings;
	
	public DefaultResult(Long affectedRows, List<String> warnings) {
		this.affectedRows = affectedRows;
		this.warnings = warnings;
	}
	
	public Long getAffectedRows() {
		return affectedRows;
	}

	public DbFuture<ResultSet> getGeneratedKeys() {
		// TODO Implement me
		throw new IllegalStateException("Not yet implemented");
	}

	public List<String> getWarnings() {
		// TODO Auto-generated method stub
		return null;
	}

}
