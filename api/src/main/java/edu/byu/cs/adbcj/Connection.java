package edu.byu.cs.adbcj;

import java.io.Closeable;

public interface Connection extends Closeable, Session {

	ConnectionManager getConnectionManager();
	
}
