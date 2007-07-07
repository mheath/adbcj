package edu.byu.cs.adbcj;

import java.io.Closeable;

/**
 * 
 * 
 * @author Mike Heath
 */
public interface ConnectionManager extends Closeable {
	
	DbFuture<Connection> connect();
	
	public void close() throws DbException;

}
