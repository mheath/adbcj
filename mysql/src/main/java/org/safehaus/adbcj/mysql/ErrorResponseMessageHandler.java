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

import org.safehaus.adbcj.support.AbstractDbFutureListenerSupport;
import org.safehaus.adbcj.support.Request;

public class ErrorResponseMessageHandler implements MessageHandler<ErrorResponse> {

	public void messageReceived(IoSession session, ErrorResponse message) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		try {
			Request<?> activeRequest = connection.getActiveRequest();
			AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
			future.setException(new DbException(message.getMessage()));
			future.setDone();
		} finally {
			connection.makeNextRequestActive();
		}
	}

}
