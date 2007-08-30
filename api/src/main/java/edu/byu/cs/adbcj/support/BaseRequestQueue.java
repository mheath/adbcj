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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import edu.byu.cs.adbcj.Session;

public abstract class BaseRequestQueue implements Session {

	private final Queue<Request<?>> requestQueue = new LinkedList<Request<?>>();
	
	private Request<?> activeRequest;
	
	protected synchronized <E> DefaultDbSessionFuture<E> enqueueRequest(final RequestAction<E> action) {
		final Request<E> request = new Request<E>(action);
		
		DefaultDbSessionFuture<E> future = new DefaultDbSessionFuture<E>(this) {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				if (removeRequest(request)) {
					return true;
				}
				return request.getAction().cancel(mayInterruptIfRunning);
			}
		};
		
		request.setFuture(future);
		
		if (activeRequest == null) {
			setActiveRequest(request);
		} else {
			requestQueue.add(request);
		}
		return future;
	}
	
	protected synchronized boolean removeRequest(Request<?> request) {
		if (request.getAction().canRemove()) {
			return requestQueue.remove(request);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected synchronized <E> Request<E> makeNextRequestActive() {
		Request<E> request = (Request<E>)requestQueue.poll();
		setActiveRequest(request);
		return request;
	}
	
	@SuppressWarnings("unchecked")
	protected <E> Request<E> getActiveRequest() {
		return (Request<E>)activeRequest;
	}
	
	protected synchronized void cancelPendingRequests(boolean mayInterruptIfRunning) {
		for (Iterator<Request<?>> i = requestQueue.iterator(); i.hasNext();) {
			Request<?> request = i.next();
			request.getFuture().cancel(mayInterruptIfRunning);
			i.remove();
		}
	}
	
	private <T> void setActiveRequest(Request<T> request) {
		activeRequest = request;
		if (request != null) {
			request.getAction().execute(request.getFuture());
		}
	}
	
	public class Request<T> {
		private DefaultDbSessionFuture<T> future = null;
		private final RequestAction<T> action;
		
		private Object payload;
		
		private Request(RequestAction<T> action) {
			this.action = action;
		}

		public DefaultDbFuture<T> getFuture() {
			return future;
		}

		public void setFuture(DefaultDbSessionFuture<T> future) {
			if (this.future != null) {
				throw new IllegalStateException("future can only be set once");
			}
			this.future = future;
		}

		public RequestAction<T> getAction() {
			return action;
		}

		public Object getPayload() {
			return payload;
		}

		public void setPayload(Object payload) {
			this.payload = payload;
		}

	}
}
