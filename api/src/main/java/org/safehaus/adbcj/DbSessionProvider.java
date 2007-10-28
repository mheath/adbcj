package org.safehaus.adbcj;

public interface DbSessionProvider {

	DbFuture<? extends DbSession> connect();
	
}
