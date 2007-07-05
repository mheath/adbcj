package edu.byu.cs.adbcj;

public interface SessionPool {

	DbFuture<Session> getSession();
	
}
