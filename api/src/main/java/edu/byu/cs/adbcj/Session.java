package edu.byu.cs.adbcj;

public interface Session {

	void beginTransaction();
	
	DbSessionFuture<Void> commit();
	
	DbSessionFuture<Void> rollback();
	
	boolean isInTransaction();
	
	TransactionIsolationLevel getTransactionIsolationLevel();
	
	void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel);
	
	DbSessionFuture<ResultSet> executeQuery(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(String sql);
	
	DbSessionFuture<Void> close(boolean immediate) throws DbException;
	
	boolean isClosed();

}
