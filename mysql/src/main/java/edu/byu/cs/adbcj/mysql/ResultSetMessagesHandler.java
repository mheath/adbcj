package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;
import edu.byu.cs.adbcj.support.BaseRequestQueue.Request;

public class ResultSetMessagesHandler<T extends Response> implements MessageHandler<T> {

	@SuppressWarnings("unchecked")
	public void messageReceived(IoSession session, Response message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request activeRequest = connection.getActiveRequest();
		MysqlResultSet resultSet = (MysqlResultSet)activeRequest.getPayload();
		
		if (message instanceof ResultSetResponse) {
			ResultSetResponse resultSetResponse = (ResultSetResponse)message;
			if (resultSet != null) {
				throw new DbException("Already processing a result set");
			}
			
			resultSet = new MysqlResultSet(resultSetResponse.getFieldCount());
			activeRequest.setPayload(resultSet);
		} else if (message instanceof ResultSetFieldResponse) {
			ResultSetFieldResponse fieldResponse = (ResultSetFieldResponse)message;
			resultSet.addField(fieldResponse.getField());
		} else if (message instanceof ResultSetRowResponse) {
			ResultSetRowResponse rowResponse = (ResultSetRowResponse)message;
			resultSet.addResult(rowResponse.getRow());
		} else if (message instanceof EofResponse) {
			EofResponse eof = (EofResponse)message;
			switch (eof.getType()) {
			case FIELD:
				if (resultSet.getFieldCount() != resultSet.getFields().size()) {
					throw new MysqlException("Did not read the specified number of fields");
				}
				break;
			case ROW:
				AbstractDbFutureBase<ResultSet> currentFuture = (AbstractDbFutureBase<ResultSet>)activeRequest.getFuture();
				currentFuture.setValue(resultSet);
				currentFuture.setDone();
				break;
			default:
				throw new MysqlException("Unkown eof response type");
			}
		} else {
			throw new IllegalStateException("Don't know how to handle message of type " + message.getClass().getName());
		}
		
	}

}
