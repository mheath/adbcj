package edu.byu.cs.adbcj.support;

import java.util.LinkedList;
import java.util.List;

public class TransactionHelper {

	private boolean started = false;
	private List<BaseRequestQueue.Request<?>> requests = new LinkedList<BaseRequestQueue.Request<?>>();
	
	public boolean isStarted() {
		return started;
	}
	
	public void setStarted(boolean started) {
		this.started = started;
	}
	
	public List<BaseRequestQueue.Request<?>> getRequests() {
		return requests;
	}
	
	public void addRequests(BaseRequestQueue.Request<?> request) {
		requests.add(request);
	}
	
	
}
