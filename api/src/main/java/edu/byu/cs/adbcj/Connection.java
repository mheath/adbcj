package edu.byu.cs.adbcj;


public interface Connection extends Session {

	ConnectionManager getConnectionManager();
	
	DbFuture<Void> ping();
	
}
