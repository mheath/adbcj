package edu.byu.cs.adbcj.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public abstract class BaseRequestQueue {

	private final Queue<Request> requestQueue = new LinkedList<Request>();
	
	private Request activeRequest;
	
	protected synchronized <E> AbstractDbFutureBase<E> enqueueRequest(final RequestAction action) {
		final Request request = new Request(action);
		
		AbstractDbFutureBase<E> future = new AbstractDbFutureBase<E>() {
			@Override
			protected boolean doCancel(boolean mayInterruptIfRunning) {
				if (removeRequest(request)) {
					return true;
				}
				return request.getAction().cancle(mayInterruptIfRunning);
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
	
	private synchronized boolean removeRequest(Request request) {
		return requestQueue.remove(request);
	}

	protected synchronized Request makeNextRequestActive() {
		Request request = requestQueue.poll();
		setActiveRequest(request);
		return request;
	}
	
	protected Request getActiveRequest() {
		return activeRequest;
	}
	
	protected synchronized void cancelPendingRequests(boolean mayInterruptIfRunning) {
		for (Iterator<Request> i = requestQueue.iterator(); i.hasNext();) {
			Request request = i.next();
			request.getFuture().cancel(mayInterruptIfRunning);
			i.remove();
		}
	}
	
	private void setActiveRequest(Request request) {
		activeRequest = request;
		if (request != null) {
			request.getAction().execute();
		}
	}
	
	public class Request {
		private AbstractDbFutureBase<?> future = null;
		private final RequestAction action;
		
		private Object payload;
		
		private Request(RequestAction action) {
			this.action = action;
		}

		public AbstractDbFutureBase<?> getFuture() {
			return future;
		}

		public void setFuture(AbstractDbFutureBase<?> future) {
			if (this.future != null) {
				throw new IllegalStateException("future can only be set once");
			}
			this.future = future;
		}

		public RequestAction getAction() {
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
