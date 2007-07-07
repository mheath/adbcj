package edu.byu.cs.adbcj.mysql;

import java.util.EnumSet;
import java.util.Set;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;
import edu.byu.cs.adbcj.support.AbstractDbSessionFutureBase;

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
		// TODO Refactor this to use demux protocol handler
		
		System.out.println("Message recieved: " + message);
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		if (message instanceof ServerGreeting) {
			ServerGreeting serverGreeting = (ServerGreeting)message;
			
			System.out.println(serverGreeting.getVersion());
			connection.setServerGreeting(serverGreeting);
			
			// Send Login request
			connection.setState(State.AUTHENTICATING);
			LoginRequest request = new LoginRequest(connection.getCredentials(), CLIENT_CAPABILITIES, EXTENDED_CLIENT_CAPABILITIES, connection.getCharacterSet());
			session.write(request);
		}
		
		if (message instanceof OkResponse) {
			AbstractDbFutureListenerSupport currentFuture = connection.getCurrentFuture();
			if (currentFuture != null) {
				currentFuture.setDone();
			}
		}
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		System.out.println("Session created"); // TODO Replace with logging
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		AbstractDbSessionFutureBase<Void> closeFuture = connection.getCloseFuture();
		if (closeFuture != null) {
			closeFuture.setDone();
		}
		System.out.println("Session closed"); // TODO Replace with logging
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		// TODO: Implement properly
		cause.printStackTrace();
	}
	
}
