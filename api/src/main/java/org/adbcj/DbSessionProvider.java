package org.adbcj;

public interface DbSessionProvider {

	DbFuture<? extends DbSession> connect();
	
}
