package edu.byu.cs.adbcj.support;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.byu.cs.adbcj.DbException;

public abstract class AbstractDbFutureBase<T> extends AbstractDbFutureListenerSupport<T> {

	private interface AwaitMethod {
		void await() throws InterruptedException;
	}

	private volatile boolean cancelled = false;
	private volatile T value;

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || isDone()) {
			return false;
		}
		getLock().lock();
		if (cancelled || isDone()) {
			return false;
		}
		try {
			cancelled = doCancel(mayInterruptIfRunning);
			if (cancelled) {
				setDone();
			}
		} finally {
			getLock().unlock();
		}
		return cancelled;
	}

	public T get() throws DbException, InterruptedException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				getCondition().await();
			}
		});
	}

	public T get(final long timeout, final TimeUnit unit) throws DbException, InterruptedException, TimeoutException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				getCondition().await(timeout, unit);
			}
		});
	}
	
	private T doGet(AwaitMethod awaitMethod) throws InterruptedException {
		if (cancelled) {
			throw new CancellationException();
		}
		if (isDone()) {
			if (getException() != null) {
				throw getException();
			}
			return value;
		}
		getLock().lock();
		try {
			if (cancelled) {
				throw new CancellationException();
			}
			if (isDone()) {
				if (getException() != null) {
					throw getException();
				}
				return value;
			}
			awaitMethod.await();
			if (cancelled) {
				throw new CancellationException();
			}
			if (getException() != null) {
				throw getException();
			}
			return value;
		} finally {
			getLock().unlock();
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setValue(T value) throws IllegalStateException {
		getLock().lock();
		try {
			if (isDone()) {
				throw new IllegalStateException("Cannot set value when future object is done");
			}
			this.value = value;
		} finally {
			getLock().unlock();
		}
	}
	
	protected abstract boolean doCancel(boolean mayInterruptIfRunning);
	
}
