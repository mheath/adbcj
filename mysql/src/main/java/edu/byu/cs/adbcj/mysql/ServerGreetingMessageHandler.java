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

public class ServerGreetingMessageHandler implements MessageHandler<ServerGreeting> {

	public void messageReceived(IoSession session, ServerGreeting serverGreeting) throws Exception {
		MysqlConnection connection = IoSessionUtil.getMysqlConnection(session);

		System.out.println(serverGreeting.getVersion());

		// Save server greeting
		connection.setServerGreeting(serverGreeting);
		
		// Send Login request
		LoginRequest request = new LoginRequest(connection.getCredentials(), connection.getClientCapabilities(), connection.getExtendedClientCapabilities(), connection.getCharacterSet());
		session.write(request);
	}

}
