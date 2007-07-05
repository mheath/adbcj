package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;

public class IoSessionUtil {

	public static Object getSessionAttribute(IoSession session, SessionId sessionId) {
		return session.getAttribute(sessionId.toString());
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getSessionAttribute(IoSession session, SessionId sessionId, T defaultValue) {
		return (T)session.getAttribute(sessionId.toString(), defaultValue);
	}
	
	public static void setSessionAttribute(IoSession session, SessionId sessionId, Object value) {
		if (session == null) {
			throw new IllegalArgumentException("session cannot be null");
		}
		session.setAttribute(sessionId.toString(), value);
	}
	
}
