package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;

public class ErrorResponseMessageHandler implements MessageHandler<ErrorResponse> {

	@SuppressWarnings("unchecked")
	public void messageReceived(IoSession session, ErrorResponse message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		AbstractDbFutureListenerSupport currentFuture = connection.getCurrentFuture();
		if (currentFuture != null) {
			currentFuture.setException(new DbException(message.getMessage()));
			currentFuture.setDone();
		}
	}

}
