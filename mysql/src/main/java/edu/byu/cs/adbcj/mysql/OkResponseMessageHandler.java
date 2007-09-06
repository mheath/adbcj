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

import java.util.LinkedList;
import java.util.List;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.support.DefaultDbFuture;
import edu.byu.cs.adbcj.support.DefaultResult;
import edu.byu.cs.adbcj.support.AbstractSessionRequestQueue.Request;

public class OkResponseMessageHandler implements MessageHandler<OkResponse> {
	private Logger logger = LoggerFactory.getLogger(OkResponseMessageHandler.class);
	
	public void messageReceived(IoSession session, OkResponse response) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);
		
		logger.info("Response '{}' on connection {}", response, connection);
				
		Request<?> activeRequest = connection.getActiveRequest();
		if (activeRequest == null) {
			throw new IllegalStateException("Received response with no activeRequest " + response);
		}
		DefaultDbFuture future = activeRequest.getFuture();
		List<String> warnings = null;
		if (response.getWarningCount() > 0) {
			warnings = new LinkedList<String>();
			for (int i = 0; i < response.getWarningCount(); i++) {
				warnings.add(response.getMessage());
			}
		}
		Result result = new DefaultResult(response.getAffectedRows(), warnings);
		future.setValue(result);
		future.setDone();
		connection.makeNextRequestActive();
	}

}
