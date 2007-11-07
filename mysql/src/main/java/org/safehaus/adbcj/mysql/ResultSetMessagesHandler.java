/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.safehaus.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.support.DefaultDbFuture;
import org.safehaus.adbcj.support.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultSetMessagesHandler<T extends Response> implements MessageHandler<T> {
	
	private final Logger logger = LoggerFactory.getLogger(ResultSetMessagesHandler.class); 

	@SuppressWarnings("unchecked")
	public void messageReceived(IoSession session, Response message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request<?> activeRequest = connection.getActiveRequest();
		MysqlResultSet resultSet = (MysqlResultSet)activeRequest.getPayload();
		
		if (message instanceof ResultSetResponse) {
			ResultSetResponse resultSetResponse = (ResultSetResponse)message;
			if (resultSet != null) {
				throw new DbException(connection, "Already processing a result set");
			}
			
			logger.debug("Creating result set");
			resultSet = new MysqlResultSet(connection, resultSetResponse.getFieldCount());
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
					throw new MysqlException(connection, "Did not read the specified number of fields");
				}
				break;
			case ROW:
				DefaultDbFuture<ResultSet> currentFuture = (DefaultDbFuture<ResultSet>)activeRequest.getFuture();
				if (!currentFuture.isCancelled()) {
					currentFuture.setValue(resultSet);
					currentFuture.setDone();
				}
				connection.makeNextRequestActive();
				break;
			default:
				throw new MysqlException(connection, "Unkown eof response type");
			}
		} else {
			throw new IllegalStateException("Don't know how to handle message of type " + message.getClass().getName());
		}
		
	}

}
