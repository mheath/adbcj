package edu.byu.cs.adbcj;

import java.io.Closeable;

public interface Connection extends Closeable {

	DbFuture<ResultSet> execute(String sql);
	
	DbFuture<PreparedStatement> prepareStatement(String sql);
	
	public void close() throws DbException;
	
}
