package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;
import edu.byu.cs.adbcj.support.BaseRequestQueue.Request;

public class OkResponseMessageHandler implements MessageHandler<OkResponse> {

	public void messageReceived(IoSession session, OkResponse response) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request activeRequest = connection.getActiveRequest();
		AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
		if (future != null) {
			// TODO: Determine a mechanism for setting the future's value
			future.setDone();
			connection.makeNextRequestActive();
		}
	}

}
