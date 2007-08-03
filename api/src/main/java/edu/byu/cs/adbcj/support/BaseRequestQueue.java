package edu.byu.cs.adbcj.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public abstract class BaseRequestQueue {

	private final Queue<Request<?>> requestQueue = new LinkedList<Request<?>>();
	
	private Request<?> activeRequest;
	
	protected synchronized <E> AbstractDbFutureBase<E> enqueueRequest(final RequestAction<E> action) {
		final Request<E> request = new Request<E>(action);
		
		AbstractDbFutureBase<E> future = new AbstractDbFutureBase<E>() {
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
	
	private synchronized boolean removeRequest(Request<?> request) {
		return requestQueue.remove(request);
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
		private AbstractDbFutureBase<T> future = null;
		private final RequestAction<T> action;
		
		private Object payload;
		
		private Request(RequestAction<T> action) {
			this.action = action;
		}

		public AbstractDbFutureBase<T> getFuture() {
			return future;
		}

		public void setFuture(AbstractDbFutureBase<T> future) {
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
