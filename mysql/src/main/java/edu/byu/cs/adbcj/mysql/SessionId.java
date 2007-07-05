package edu.byu.cs.adbcj.mysql;

enum SessionId {
	CONNECT_FUTURE,
	SERVER_GREETING,
	STATE,
	CREDENTIALS;
	
	@Override
	public String toString() {
		return SessionId.class.getName() + "." + name();
	}
}
