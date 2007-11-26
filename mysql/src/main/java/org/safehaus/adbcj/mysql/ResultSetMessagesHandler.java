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
import org.safehaus.adbcj.Value;
import org.safehaus.adbcj.support.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultSetMessagesHandler<T extends Response> implements MessageHandler<T> {
	
	private final Logger logger = LoggerFactory.getLogger(ResultSetMessagesHandler.class); 

	@SuppressWarnings("unchecked")
	public void messageReceived(IoSession session, T message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request<T> activeRequest = connection.getActiveRequest();
		if (message instanceof ResultSetResponse) {
			logger.debug("Creating result set");
			activeRequest.getEventHandler().startFields(activeRequest.getAccumulator());
		} else if (message instanceof ResultSetFieldResponse) {
			ResultSetFieldResponse fieldResponse = (ResultSetFieldResponse)message;
			activeRequest.getEventHandler().field(fieldResponse.getField(), activeRequest.getAccumulator());
		} else if (message instanceof ResultSetRowResponse) {
			ResultSetRowResponse rowResponse = (ResultSetRowResponse)message;

			activeRequest.getEventHandler().startRow(activeRequest.getAccumulator());
			for (Value value : rowResponse.getValues()) {
				activeRequest.getEventHandler().value(value, activeRequest.getAccumulator());
			}
			activeRequest.getEventHandler().endRow(activeRequest.getAccumulator());
		} else if (message instanceof EofResponse) {
			EofResponse eof = (EofResponse)message;
			switch (eof.getType()) {
			case FIELD:
				activeRequest.getEventHandler().endFields(activeRequest.getAccumulator());
				break;
			case ROW:
				activeRequest.getEventHandler().endResults(activeRequest.getAccumulator());
				connection.makeNextRequestActive();
				break;
			default:
				throw new MysqlException(connection, "Unkown eof response type");
			}
		} else {
			throw new IllegalStateException("Don't know how to handle message of type " + message.getClass().getName());
		}
		
	}

	public void messageSent(IoSession session, T message) throws Exception {
	}

}
