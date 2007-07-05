package edu.byu.cs.adbcj;

public interface DbSessionFuture<T> extends DbFuture<T> {

	Session getSession();
	
}
