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
package org.adbcj.mysql.mina;

import org.adbcj.Connection;
import org.adbcj.mysql.codec.AbstractMySqlConnection;
import org.adbcj.mysql.codec.ClientRequest;
import org.adbcj.mysql.codec.LoginCredentials;
import org.adbcj.mysql.mina.MysqlConnectionManager.MysqlConnectFuture;
import org.apache.mina.core.session.IoSession;

public class MysqlConnection extends AbstractMySqlConnection implements Connection {
	private final MysqlConnectFuture connectFuture;
	private final IoSession session;

	public MysqlConnection(MysqlConnectionManager connectionManager, MysqlConnectFuture connectFuture, IoSession session, LoginCredentials credentials) {
		super(connectionManager, credentials);
		this.connectFuture = connectFuture;
		this.session = session;
	}

	@Override
	public void write(ClientRequest request) {
		session.write(request);
	}

	@Override
	protected boolean isTransportClosing() {
		return session.isClosing();
	}

	@Override
	public MysqlConnectFuture getConnectFuture() {
		return connectFuture;
	}

}
