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
package org.adbcj.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.adbcj.DbException;
import org.adbcj.DbSession;
import org.adbcj.ResultEventHandler;

public abstract class AbstractSessionRequestQueue implements DbSession {
	
	private final Queue<Request<?>> requestQueue = new LinkedList<Request<?>>(); // Access must by synchronized on lock
	
	private final Object lock = this;
	
	private Request<?> activeRequest; // Access must by synchronized on lock
	
	protected <E> void enqueueRequest(final Request<E> request) {
		synchronized (lock) {
			boolean isEmpty = requestQueue.peek() == null && activeRequest == null;
			requestQueue.add(request);
			if (isEmpty) {
				makeNextRequestActive();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final <E> Request<E> makeNextRequestActive() {
		Request<E> request;
		synchronized (lock) {
			if (activeRequest != null && !activeRequest.isDone()) {
				throw new DbException("Active request is not done: " + activeRequest);
			}
			request = (Request<E>)requestQueue.poll();
			activeRequest = request;
		}
		if (request != null) {
			try {
				request.execute();
			} catch (Throwable e) {
				request.error(DbException.wrap(this, e));
			}
		}
		return request;
	}
	
	@SuppressWarnings("unchecked")
	protected <E> Request<E> getActiveRequest() {
		synchronized (lock) {
			return (Request<E>)activeRequest;
		}
	}
	
	protected void cancelPendingRequests(boolean mayInterruptIfRunning) {
		synchronized (lock) {
			for (Iterator<Request<?>> i = requestQueue.iterator(); i.hasNext();) {
				Request<?> request = i.next();
				i.remove();
				request.cancel(mayInterruptIfRunning);
			}
		}
	}
	
	public abstract class Request<T> extends DefaultDbSessionFuture<T> {
		
		private Object payload;
		private final ResultEventHandler<T> eventHandler;
		private final T accumulator;
		private Object transaction;
		
		public Request() {
			this(null, null);
		}
		
		public Request(ResultEventHandler<T> eventHandler, T accumulator) {
			super(AbstractSessionRequestQueue.this);
			this.eventHandler = eventHandler;
			this.accumulator = accumulator;
		}
		
		public abstract void execute() throws Exception;
		
		protected boolean cancelRequest(boolean mayInterruptIfRunning) {
			return true;
		}
		
		public final boolean doCancel(boolean mayInterruptIfRunning) {
			boolean cancelled = cancelRequest(mayInterruptIfRunning);
			
			// The the request was cancelled and it can be removed
			if (cancelled && canRemove()) {
				synchronized (lock) {
					// Remove the quest and if the removal was successful and this request is active, go to the next request
					if (requestQueue.remove(this) && this == activeRequest) {
						makeNextRequestActive();
					}
				}
			}
			return cancelled;
		}
		
		public boolean canRemove() {
			return true;
		}

		public Object getPayload() {
			return payload;
		}

		public void setPayload(Object payload) {
			this.payload = payload;
		}

		public T getAccumulator() {
			return accumulator;
		}
		
		public ResultEventHandler<T> getEventHandler() {
			return eventHandler;
		}
		
		public Object getTransaction() {
			return transaction;
		}

		public void setTransaction(Object transaction) {
			this.transaction = transaction;
		}
		
		@Override
		public final void setResult(T result) {
			throw new IllegalStateException("You must call complete(T result) instead");
		}
		
		@Override
		public final void setException(Throwable exception) {
			throw new IllegalStateException("You must call error(DbException exception) instead");
		}
		
		public void complete(T result) {
			super.setResult(result);
			makeNextRequestActive();
		}
		
		public void error(DbException exception) {
			super.setException(exception);
			makeNextRequestActive();
		}
	}

}
