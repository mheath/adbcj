package org.adbcj.postgresql;

import org.apache.mina.common.IoSession;
import org.adbcj.Connection;
import org.adbcj.support.DefaultDbFuture;

public class IoSessionUtil {

	private static final String CONNECTION_KEY = IoSessionUtil.class.getName() + ".connection";

	private IoSessionUtil() {
		// Make this class uninstantiable with a private constructor.
	}

	public static PgConnection getConnection(IoSession session) {
		return (PgConnection)session.getAttribute(CONNECTION_KEY);
	}
	
	public static void setConnection(IoSession session, PgConnection connection) {
		session.setAttribute(CONNECTION_KEY, connection);
	}
	
}
