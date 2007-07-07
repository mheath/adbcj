package edu.byu.cs.adbcj.support;

import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Session;

public abstract class AbstractDbSessionFutureBase<T> extends AbstractDbFutureBase<T> implements DbSessionFuture<T> {

	private final Session session;
	
	public AbstractDbSessionFutureBase(Session session) {
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}

}
