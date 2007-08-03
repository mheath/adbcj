package edu.byu.cs.adbcj.support;

public interface RequestAction<T> {
	
	void execute(AbstractDbFutureBase<T> future);
	
	boolean cancel(boolean mayInterruptIfRunning);
	
}
