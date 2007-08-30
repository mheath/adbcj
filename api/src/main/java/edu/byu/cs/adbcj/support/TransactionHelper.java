package edu.byu.cs.adbcj.support;

import java.util.LinkedList;
import java.util.List;

public class TransactionHelper {

	private boolean started = false;
	private List<AbstractSessionRequestQueue.Request<?>> requests = new LinkedList<AbstractSessionRequestQueue.Request<?>>();
	
	public boolean isStarted() {
		return started;
	}
	
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	public List<AbstractSessionRequestQueue.Request<?>> getRequests() {
		return requests;
	}
	
	public void addRequests(AbstractSessionRequestQueue.Request<?> request) {
		requests.add(request);
	}
	
	
}
