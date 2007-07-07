package edu.byu.cs.adbcj.support;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;

public abstract class AbstractDbFutureListenerSupport<T> implements DbFuture<T> {

	private final List<DbListener<T>> listeners = new LinkedList<DbListener<T>>();
	
	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	private volatile boolean done = false;
	private volatile DbException exception;

	public void addListener(DbListener<T> listener) {
		lock.lock();
		try {
			if (done) {
				if (exception == null) {
					try {
						listener.onCompletion(this);
					} catch (Exception e) {
						// TODO: Handle exception
						e.printStackTrace();
					}
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

	public boolean isDone() {
		return done;
	}

	public void setDone() {
		this.done = true;
		lock.lock();
		try {
			condition.signalAll();
			if (exception != null) {
				for (DbListener<T> listener : getListeners()) {
					try {
						listener.onCompletion(this);
					} catch (Exception e) {
						// TODO: Handle this exception in connection manager
						e.printStackTrace();
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	protected DbException getException() {
		return exception;
	}

	public void setException(DbException exception) {
		lock.lock();
		try {
			if (isDone()) {
				throw new IllegalStateException("Can not set exception when future object is done.");
			}
			this.exception = exception;
		} finally {
			lock.unlock();
		}
	}

	protected Lock getLock() {
		return lock;
	}

	protected Condition getCondition() {
		return condition;
	}
	
	protected List<DbListener<T>> getListeners() {
		return listeners;
	}

}