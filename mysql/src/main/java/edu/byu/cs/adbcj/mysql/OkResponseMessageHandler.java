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
package edu.byu.cs.adbcj.mysql;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import edu.byu.cs.adbcj.support.AbstractDbFutureListenerSupport;
import edu.byu.cs.adbcj.support.BaseRequestQueue.Request;

public class OkResponseMessageHandler implements MessageHandler<OkResponse> {

	public void messageReceived(IoSession session, OkResponse response) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		Request<?> activeRequest = connection.getActiveRequest();
		AbstractDbFutureListenerSupport<?> future = activeRequest.getFuture();
		if (future != null) {
			// TODO: Determine a mechanism for setting the future's value
			future.setDone();
			connection.makeNextRequestActive();
		}
	}

}
