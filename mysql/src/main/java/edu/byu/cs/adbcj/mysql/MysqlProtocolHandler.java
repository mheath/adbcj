package edu.byu.cs.adbcj.mysql;

import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class MysqlProtocolHandler extends IoHandlerAdapter {
	
	private static final Set<ClientCapabilities> CLIENT_CAPABILITIES = EnumSet.of(
			ClientCapabilities.LONG_PASSWORD,
			ClientCapabilities.FOUND_ROWS,
			ClientCapabilities.LONG_COLUMN_FLAG,
			ClientCapabilities.CONNECT_WITH_DB,
			ClientCapabilities.LOCAL_FILES,
			ClientCapabilities.PROTOCOL_4_1,
			ClientCapabilities.TRANSACTIONS,
			ClientCapabilities.SECURE_AUTHENTICATION);
	
	private static final Set<ExtendedClientCapabilities> EXTENDED_CLIENT_CAPABILITIES = EnumSet.of(
			ExtendedClientCapabilities.MULTI_RESULTS
			);

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		if (message instanceof ServerGreeting) {
			System.out.println(((ServerGreeting)message).getVersion());
			IoSessionUtil.setSessionAttribute(session, SessionId.SERVER_GREETING, message);
			IoSessionUtil.setSessionAttribute(session, SessionId.STATE, State.AUTHENTICATING);
			
			// Send Login request
			LoginCredentials credentials = (LoginCredentials)IoSessionUtil.getSessionAttribute(session, SessionId.CREDENTIALS);
			LoginRequest request = new LoginRequest(credentials, CLIENT_CAPABILITIES, EXTENDED_CLIENT_CAPABILITIES);
			session.write(request);
		}
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		System.out.println("Session created");
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		System.out.println("Session closed");
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		// TODO: Implement properly
		cause.printStackTrace();
	}
	
}
