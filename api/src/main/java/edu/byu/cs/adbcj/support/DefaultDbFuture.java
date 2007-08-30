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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.byu.cs.adbcj.DbException;

public class DefaultDbFuture<T> extends AbstractDbFutureListenerSupport<T> {

	private interface AwaitMethod {
		void await() throws InterruptedException;
	}

	private volatile boolean cancelled = false;
	private volatile T value;

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || isDone()) {
			return false;
		}
		getLock().lock();
		if (cancelled || isDone()) {
			return false;
		}
		try {
			cancelled = doCancel(mayInterruptIfRunning);
			if (cancelled) {
				setDone();
			}
		} finally {
			getLock().unlock();
		}
		return cancelled;
	}

	public T get() throws DbException, InterruptedException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				getCondition().await();
			}
		});
	}

	public T get(final long timeout, final TimeUnit unit) throws DbException, InterruptedException, TimeoutException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				getCondition().await(timeout, unit);
			}
		});
	}
	
	private T doGet(AwaitMethod awaitMethod) throws InterruptedException {
		if (cancelled) {
			throw new CancellationException();
		}
		if (isDone()) {
			if (getException() != null) {
				throw getException();
			}
			return value;
		}
		getLock().lock();
		try {
			if (cancelled) {
				throw new CancellationException();
			}
			if (isDone()) {
				if (getException() != null) {
					throw getException();
				}
				return value;
			}
			awaitMethod.await();
			if (cancelled) {
				throw new CancellationException();
			}
			if (getException() != null) {
				throw getException();
			}
			return value;
		} finally {
			getLock().unlock();
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setValue(T value) throws IllegalStateException {
		getLock().lock();
		try {
			if (isDone()) {
				throw new IllegalStateException("Cannot set value when future object is done");
			}
			this.value = value;
		} finally {
			getLock().unlock();
		}
	}
	
	protected boolean doCancel(boolean mayInterruptIfRunning) {
		return false;
	}
	
}
