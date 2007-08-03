package edu.byu.cs.adbcj.support;

import java.util.concurrent.Future;

import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Session;

public class ConcurrentFutureSessionProxy<T> extends ConcurrentFutureProxy<T> implements DbSessionFuture<T> {

	private final Session session;
	
	public ConcurrentFutureSessionProxy(Session session) {
		this.session = session;
	}
	
	public ConcurrentFutureSessionProxy(Future<T> future, Session session) {
		super(future);
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}

}
