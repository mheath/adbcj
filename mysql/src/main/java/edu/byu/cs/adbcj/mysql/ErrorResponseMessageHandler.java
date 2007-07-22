package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;
import edu.byu.cs.adbcj.support.BaseRequestQueue.Request;

public class ErrorResponseMessageHandler implements MessageHandler<ErrorResponse> {

	public void messageReceived(IoSession session, ErrorResponse message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request activeRequest = connection.getActiveRequest();
		AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
		if (future != null) {
			future.setException(new DbException(message.getMessage()));
			future.setDone();
			connection.makeNextRequestActive();
		}
	}

}
