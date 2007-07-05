package edu.byu.cs.adbcj;

import java.io.Closeable;

public interface Session extends Closeable {

	DbSessionFuture<Void> beginTransaction();
	
	DbSessionFuture<Void> commit();
	
	DbSessionFuture<Void> rollback();
	
	boolean isInTransaction();
	
	TransactionIsolationLevel getTransactionIsolationLevel();
	
	void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel);
	
	DbSessionFuture<ResultSet> executeQuery(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(String sql);
	
	public void close() throws DbException;

}
