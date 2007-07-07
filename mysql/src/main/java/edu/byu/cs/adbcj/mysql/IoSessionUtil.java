package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;

public class IoSessionUtil {
	
	private static final String CONNECTION_KEY = MysqlConnection.class.getName();

	public static MysqlConnection getMysqlConnection(IoSession session) {
		return (MysqlConnection)session.getAttribute(CONNECTION_KEY);
	}
	
	public static void setMysqlConnection(IoSession session, MysqlConnection connection) {
		session.setAttribute(CONNECTION_KEY, connection);
	}
}
