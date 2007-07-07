package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;

public class OkResponseMessageHandler implements MessageHandler<OkResponse> {

	@SuppressWarnings("unchecked")
	public void messageReceived(IoSession session, OkResponse response) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		AbstractDbFutureListenerSupport currentFuture = connection.getCurrentFuture();
		if (currentFuture != null) {
			currentFuture.setDone();
			connection.setCurrentFuture(null);
		}
	}

}
