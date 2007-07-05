package edu.byu.cs.adbcj.support;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;

public abstract class BaseDbFuture<T> implements DbFuture<T> {

	private interface AwaitMethod {
		void await() throws InterruptedException;
	}

	private final List<DbListener<T>> listeners = new LinkedList<DbListener<T>>();

	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();
	
	private volatile boolean cancelled = false;
	private volatile boolean done = false;
	
	private volatile T value;
	private volatile DbException exception;

	public void addListener(DbListener<T> listener) {
		lock.lock();
		try {
			if (done) {
				if (exception == null) {
					listener.onCompletion(this);
				}
			}
			listeners.add(listener);
		} finally {
			lock.unlock();
		}
	}

	public void removeListener(DbListener<T> listener) {
		lock.lock();
		try {
			listeners.remove(listener);
		} finally {
			lock.unlock();
		}
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || done) {
			return false;
		}
		lock.lock();
		if (cancelled) {
			return false;
		}
		try {
			cancelled = doCancel(mayInterruptIfRunning);
			if (cancelled) {
				done = true;
				condition.signalAll();
			}
		} finally {
			lock.unlock();
		}
		return cancelled;
	}

	public T get() throws InterruptedException, ExecutionException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				condition.await();
			}
		});
	}

	public T get(final long timeout, final TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return doGet(new AwaitMethod() {
			public void await() throws InterruptedException {
				condition.await(timeout, unit);
			}
		});
	}
	
	private T doGet(AwaitMethod awaitMethod) throws InterruptedException {
		if (cancelled) {
			throw new CancellationException();
		}
		if (done) {
			if (exception != null) {
				throw exception;
			}
			return value;
		}
		lock.lock();
		try {
			if (cancelled) {
				throw new CancellationException();
			}
			if (done) {
				if (exception != null) {
					throw exception;
				}
				return value;
			}
			awaitMethod.await();
			if (cancelled) {
				throw new CancellationException();
			}
			if (exception != null) {
				throw exception;
			}
			return value;
		} finally {
			lock.unlock();
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isDone() {
		return done;
	}

	public void setValue(T value) {
		try {
			lock.lock();
			this.value = value;
			done = true;
			try {
				for (DbListener<T> listener : listeners) {
					listener.onCompletion(this);
				}
			} catch (RuntimeException e) {
				exception = new DbException("Error invoking completion listeners", e);
			}
			condition.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	public void setException(DbException exception) {
		lock.lock();
		try {
			this.exception = exception;
			done = true;
			condition.signalAll();
		} finally {
			lock.unlock();
		}
	}
	
	protected abstract boolean doCancel(boolean mayInterruptIfRunning);
	
}
