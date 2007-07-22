package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.DemuxingIoHandler;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;
import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;
import edu.byu.cs.adbcj.support.BaseRequestQueue.Request;

public class MysqlProtocolHandler extends DemuxingIoHandler {
	
	@SuppressWarnings("unchecked")
	public MysqlProtocolHandler() {
		addMessageHandler(ServerGreeting.class, new ServerGreetingMessageHandler());
		addMessageHandler(OkResponse.class, new OkResponseMessageHandler());
		addMessageHandler(ErrorResponse.class, new ErrorResponseMessageHandler());
		
		// Add handler for result set messages
		ResultSetMessagesHandler resultSetMessagesHandler = new ResultSetMessagesHandler();
		addMessageHandler(ResultSetResponse.class, resultSetMessagesHandler);
		addMessageHandler(ResultSetFieldResponse.class, resultSetMessagesHandler);
		addMessageHandler(ResultSetRowResponse.class, resultSetMessagesHandler);
		addMessageHandler(EofResponse.class, resultSetMessagesHandler);
	}
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		System.out.println("Session created"); // TODO Replace with logging
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		AbstractDbFutureBase<Void> closeFuture = connection.getCloseFuture();
		if (closeFuture != null) {
			closeFuture.setDone();
		}
		System.out.println("Session closed"); // TODO Replace with logging
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		Request activeRequest = connection.getActiveRequest();
		if (activeRequest == null) {
			// TODO Figure out what to do with the exception
			cause.printStackTrace();
		} else {
			AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
			if (future != null) {
				if (cause instanceof DbException) {
					future.setException((DbException)cause);
				} else {
					future.setException(new DbException(cause));
				}
				future.setDone();
				connection.makeNextRequestActive();
			}
		}
	}
	
}
