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

public class IoSessionUtil {
	
	private static final String CONNECTION_KEY = IoSessionUtil.class.getName() + ".connection";

	public static MysqlConnection getMysqlConnection(IoSession session) {
		// Get and return the connection
		MysqlConnection connection = (MysqlConnection)session.getAttribute(CONNECTION_KEY);
		if (connection == null) {
			throw new IllegalStateException("MysqlConnection object was not set in the IoSession.");
		}
		return connection;
	}
	
	public static void setMysqlConnection(IoSession session, MysqlConnection connection) {
		session.setAttribute(CONNECTION_KEY, connection);
	}
}
