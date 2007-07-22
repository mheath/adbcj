package edu.byu.cs.adbcj.support;

public interface RequestAction {
	
	void execute();
	
	boolean cancle(boolean mayInterruptIfRunning);
	
}
