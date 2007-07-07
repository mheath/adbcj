package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.DemuxingIoHandler;

import edu.byu.cs.adbcj.support.AbstractDbSessionFutureBase;

public class MysqlProtocolHandler extends DemuxingIoHandler {
	
	
	public MysqlProtocolHandler() {
		addMessageHandler(ServerGreeting.class, new ServerGreetingMessageHandler());
		addMessageHandler(OkResponse.class, new OkResponseMessageHandler());
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
