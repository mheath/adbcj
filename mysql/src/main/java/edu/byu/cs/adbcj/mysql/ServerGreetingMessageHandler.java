package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

public class ServerGreetingMessageHandler implements MessageHandler<ServerGreeting> {

	public void messageReceived(IoSession session, ServerGreeting serverGreeting) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		System.out.println(serverGreeting.getVersion());
		connection.setServerGreeting(serverGreeting);
		
		// Send Login request
		connection.setState(State.AUTHENTICATING);
		LoginRequest request = new LoginRequest(connection.getCredentials(), connection.getClientCapabilities(), connection.getExtendedClientCapabilities(), connection.getCharacterSet());
		session.write(request);
	}

}
