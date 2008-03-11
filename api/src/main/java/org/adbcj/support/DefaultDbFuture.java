/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;

public class DefaultDbFuture<T> implements DbFuture<T> {

    private final Object lock;
	
    private DbListener<T> firstListener;
    private List<DbListener<T>> otherListeners;
    
    /**
     * The result of this future.
     */
    private volatile T result;
    
    /**
     * The exception thrown if there was an error.
     */
    private volatile Throwable exception;
    
    /**
     * Indicates if the future was cancelled.
     */
    private volatile boolean cancelled;
    
    /**
     * Indicates if the future has completed or not.
     */
    private volatile boolean done;
    
    /**
     * The number of threads waiting on the future.  Access must by synchronized on {@link #lock}.
     */
    private int waiters;

    public DefaultDbFuture() {
		this.lock = this;
	}
	
	public DbFuture<T> addListener(DbListener<T> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener can NOT be null");
		}
		
        boolean notifyNow = true;
        if (!done) {
	        synchronized (lock) {
	            if (!done) {
	                notifyNow = false;
	                if (firstListener == null) {
	                    firstListener = listener;
	                } else {
	                    if (otherListeners == null) {
	                        otherListeners = new ArrayList<DbListener<T>>(1);
	                    }
	                    otherListeners.add(listener);
	                }
	            }
	        }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
        return this;
	}

	public boolean removeListener(DbListener<T> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can NOT be null");
        }

        boolean removed = false;
        synchronized (lock) {
            if (!done) {
                if (listener == firstListener) {
                	removed = true;
                    if (otherListeners != null && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    removed = otherListeners.remove(listener);
                }
            }
        }

        return removed;
	}

    public final boolean cancel(boolean mayInterruptIfRunning) {
		if (done) {
			return false;
		}
		synchronized (lock) {
			if (done) {
				return false;
			}
			cancelled = doCancel(mayInterruptIfRunning);
			if (cancelled) {
				done = true;
	            if (waiters > 0) {
	                lock.notifyAll();
	            }
			}
		}
		if (cancelled) {
			notifyListeners();
		}
		return cancelled;
	}

	protected boolean doCancel(boolean mayInterruptIfRunning) {
		return false;
	}

	public T get() throws InterruptedException, DbException {
		if (done) {
			return getResult();
		}
        synchronized (lock) {
    		if (done) {
    			return getResult();
    		}
            waiters++;
            try {
                lock.wait();
            } finally {
                waiters--;
            }
        }
        return getResult();
	}

	public T get(long timeout, TimeUnit unit) throws InterruptedException, DbException, TimeoutException {
		long timeoutMillis = unit.toMillis(timeout);

		if (done) {
			return getResult();
		}
        synchronized (lock) {
    		if (done) {
    			return getResult();
    		}
            waiters++;
            try {
                lock.wait(timeoutMillis);
                if (!done) {
                	throw new TimeoutException();
                }
            } finally {
                waiters--;
            }
        }
        return getResult();
	}

	public T getUninterruptably() throws DbException {
		if (done) {
			return getResult();
		}
        synchronized (lock) {
    		if (done) {
    			return getResult();
    		}
    		boolean interrupted = false;
            waiters++;
            try {
	    		while (!done) {
	                try {
						lock.wait();
					} catch (InterruptedException e) {
						interrupted = true;
					}
	    		}
            } finally {
                waiters--;
                if (interrupted) {
                	Thread.currentThread().interrupt();
                }
            }
        }
        return getResult();
	}

	private T getResult() throws DbException {
		if (!done) {
			throw new IllegalStateException("Should not be calling this method when future is not done");
		}
		if (exception != null) {
			throw new DbException(exception);
		}
		if (cancelled) {
			throw new CancellationException();
		}
		return result;
	}

	public void setResult(T result) {
        synchronized (lock) {
            // Allow only once.
            if (done) {
                return;
            }

            this.result = result;
            done = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
        }

        notifyListeners();
	}
	
    private void notifyListener(DbListener<T> listener) {
        try {
            listener.onCompletion(this);
        } catch (Throwable t) {
        	// TODO Do something with exception
        	t.printStackTrace();
        }
    }

    private void notifyListeners() {
        // There won't be any visibility problem or concurrent modification
        // because 'ready' flag will be checked against both addListener and
        // removeListener calls.
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;

            if (otherListeners != null) {
                for (DbListener<T> l : otherListeners) {
                    notifyListener(l);
                }
                otherListeners = null;
            }
        }
    }

	public boolean isCancelled() {
		return cancelled;
	}

	public boolean isDone() {
		return done;
	}
	
	public void setException(Throwable exception) {
		synchronized (lock) {
			if (done) {
				throw new IllegalStateException("Can't set exception on completed future");
			}
			this.exception = exception;
			done = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
		}
		notifyListeners();
	}
	
}
