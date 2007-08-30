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
package edu.byu.cs.adbcj.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Session;

public class DbSessionFutureProxy<T> implements DbSessionFuture<T> {

	private final DbFuture<T> future;
	private final Session session;
	
	public DbSessionFutureProxy(DbSessionFuture<T> future) {
		this.future = future;
		this.session = future.getSession();
	}
	
	public DbSessionFutureProxy(DbFuture<T> future, Session session) {
		this.future = future;
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}

	public DbSessionFuture<T> addListener(DbListener<T> listener) {
		future.addListener(listener);
		return this;
	}

	public T get() throws DbException, InterruptedException {
		return future.get();
	}

	public T get(long timeout, TimeUnit unit) throws DbException,
			InterruptedException, TimeoutException {
		return future.get(timeout, unit);
	}

	public boolean removeListener(DbListener<T> listener) {
		return future.removeListener(listener);
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	public boolean isCancelled() {
		return future.isCancelled();
	}

	public boolean isDone() {
		return future.isDone();
	}

}