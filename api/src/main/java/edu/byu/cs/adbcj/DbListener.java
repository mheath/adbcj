package edu.byu.cs.adbcj;

public interface DbListener<T> {

	void onCompletion(DbFuture<T> listener) throws Exception;
	
}
