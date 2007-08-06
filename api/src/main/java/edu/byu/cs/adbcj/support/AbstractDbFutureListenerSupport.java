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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;

public abstract class AbstractDbFutureListenerSupport<T> implements DbFuture<T> {

	private final List<DbListener<T>> listeners = new LinkedList<DbListener<T>>();
	
	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	private volatile boolean done = false;
	private volatile DbException exception;

	public DbFuture<T> addListener(DbListener<T> listener) {
		lock.lock();
		try {
			if (done) {
				try {
					listener.onCompletion(this);
				} catch (Exception e) {
					// TODO: Handle exception
					e.printStackTrace();
				}
			}
			listeners.add(listener);
		} finally {
			lock.unlock();
		}
		return this;
	}

	public boolean removeListener(DbListener<T> listener) {
		lock.lock();
		try {
			return listeners.remove(listener);
		} finally {
			lock.unlock();
		}
	}

	public boolean isDone() {
		return done;
	}

	public void setDone() {
		this.done = true;
		lock.lock();
		try {
			condition.signalAll();
			// Invoke call-backs if future completed successfully
			for (DbListener<T> listener : getListeners()) {
				try {
					listener.onCompletion(this);
				} catch (Exception e) {
					// TODO: Handle this exception in connection manager
					e.printStackTrace();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	protected DbException getException() {
		return exception;
	}

	public void setException(DbException exception) {
		lock.lock();
		try {
			if (isDone()) {
				throw new IllegalStateException("Can not set exception when future object is done.");
			}
			this.exception = exception;
		} finally {
			lock.unlock();
		}
	}

	protected Lock getLock() {
		return lock;
	}

	protected Condition getCondition() {
		return condition;
	}
	
	protected List<DbListener<T>> getListeners() {
		return listeners;
	}

}