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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.IoSession;

public class IoSessionUtil {
	
	private static final long TIMEOUT = 5000;
	
	private static final String CONNECTION_KEY = IoSessionUtil.class.getName() + ".connection";
	private static final String LATCH_KEY = IoSessionUtil.class.getName() + ".latch";

	public static MysqlConnection getMysqlConnection(IoSession session) {
		// If we connect and the connect future's callback has not been invoked, we need to wait for the connection to get set
		
		// Get the latch used for waiting.
		CountDownLatch latch = (CountDownLatch)session.getAttribute(LATCH_KEY);
		if (latch == null) {
			throw new IllegalStateException("Latch did not get initialized when IoSession was initialized");
		}
		// Wait on the latch
		try {
			latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Timed out waiting for MysqlConnection to get set in IoSession");
		}
		
		// Get and return the connection
		MysqlConnection connection = (MysqlConnection)session.getAttribute(CONNECTION_KEY);
		if (connection == null) {
			throw new IllegalStateException("Latch was at 0 but the MysqlConnection object was not set in the IoSession.");
		}
		return connection;
	}
	
	public static void setMysqlConnection(IoSession session, MysqlConnection connection) {
		CountDownLatch latch = (CountDownLatch)session.getAttribute(LATCH_KEY);
		if (latch == null) {
			throw new IllegalStateException("Latch did not get initialized when IoSession was initialized");
		}
		session.setAttribute(CONNECTION_KEY, connection);
		latch.countDown();
	}

	public static void init(IoSession session) {
		session.setAttribute(LATCH_KEY, new CountDownLatch(1));
	}
}
