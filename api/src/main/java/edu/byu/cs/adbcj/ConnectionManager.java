package edu.byu.cs.adbcj;


/**
 * 
 * 
 * @author Mike Heath
 */
public interface ConnectionManager {
	
	DbFuture<Connection> connect();
	
	DbSessionFuture<Void> close(boolean immediate) throws DbException;
	
	boolean isClosed(); 

}
