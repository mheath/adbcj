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
package org.safehaus.adbcj.support;

import java.util.concurrent.Future;

import org.safehaus.adbcj.DbSession;
import org.safehaus.adbcj.DbSessionFuture;

public class ConcurrentFutureSessionProxy<T> extends ConcurrentFutureProxy<T> implements DbSessionFuture<T> {

	private final DbSession session;
	
	public ConcurrentFutureSessionProxy(DbSession session) {
		this.session = session;
	}
	
	public ConcurrentFutureSessionProxy(Future<T> future, DbSession session) {
		super(future);
		this.session = session;
	}
	
	public DbSession getSession() {
		return session;
	}

}
