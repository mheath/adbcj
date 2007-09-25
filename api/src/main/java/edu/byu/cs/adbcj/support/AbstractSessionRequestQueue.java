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

import edu.byu.cs.adbcj.DbSession;

public abstract class AbstractSessionRequestQueue implements DbSession {
	private final Queue<Request<?>> requestQueue = new LinkedList<Request<?>>();
	
	private Request<?> activeRequest;
	
	protected synchronized <E> DefaultDbSessionFuture<E> enqueueRequest(final Request<E> request) {
		DefaultDbSessionFuture<E> future = new DefaultDbSessionFuture<E>(this) {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				if (requestQueue.remove(request)) {
					return true;
				}
				return request.cancel(mayInterruptIfRunning);
			}
		};
		
		request.setFuture(future);
		boolean isEmpty = requestQueue.peek() == null && activeRequest == null;
		requestQueue.add(request);
		if (isEmpty) {
			makeNextRequestActive();
		}
		return future;
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
	
	private synchronized <T> void setActiveRequest(Request<T> request) {
		activeRequest = request;
		if (request != null) {
			request.execute(request.getFuture());
		}
	}
	
}
