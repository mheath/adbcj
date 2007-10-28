package org.safehaus.adbcj.postgresql;

import org.apache.mina.common.IoSession;
import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.support.DefaultDbFuture;

public class IoSessionUtil {

	private static final String CONNECT_FUTURE_KEY = IoSessionUtil.class.getName() + ".connectFuture";
	private static final String CONNECTION_KEY = IoSessionUtil.class.getName() + ".connection";

	private IoSessionUtil() {}

	@SuppressWarnings("unchecked")
	public static DefaultDbFuture<Connection> getDbConnectFuture(IoSession session) {
		return (DefaultDbFuture<Connection>)session.getAttribute(CONNECT_FUTURE_KEY);
	}
	
	public static void setDbConnectFuture(IoSession session, DefaultDbFuture<Connection> connectFuture) {
		session.setAttribute(CONNECT_FUTURE_KEY, connectFuture);
	}

	public static PgConnection getConnection(IoSession session) {
		return (PgConnection)session.getAttribute(CONNECTION_KEY);
	}
	
	public static void setConnection(IoSession session, PgConnection connection) {
		session.setAttribute(CONNECTION_KEY, connection);
	}
	
}
