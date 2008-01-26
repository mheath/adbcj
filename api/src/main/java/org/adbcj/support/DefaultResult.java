package org.adbcj.support;

import java.util.List;

import org.adbcj.DbFuture;
import org.adbcj.Result;
import org.adbcj.ResultSet;


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
		// TODO Implement DefaultResult.getGeneratedKeys()
		throw new IllegalStateException("Not yet implemented");
	}

	public List<String> getWarnings() {
		return warnings;
	}

}
