package edu.byu.cs.adbcj.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Session;

public class DbSessionFutureProxy<T> implements DbSessionFuture<T> {

	private final DbFuture<T> future;
	private final Session session;
	
	public DbSessionFutureProxy(DbFuture<T> future, Session session) {
		this.future = future;
		this.session = session;
	}
	
	public Session getSession() {
		return session;
	}

	public DbSessionFuture<T> addListener(DbListener<T> listener) {
		future.addListener(listener);
		return this;
	}

	public T get() throws DbException, InterruptedException {
		return future.get();
	}

	public T get(long timeout, TimeUnit unit) throws DbException,
			InterruptedException, TimeoutException {
		return future.get(timeout, unit);
	}

	public boolean removeListener(DbListener<T> listener) {
		return future.removeListener(listener);
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	public boolean isCancelled() {
		return future.isCancelled();
	}

	public boolean isDone() {
		return future.isDone();
	}

}