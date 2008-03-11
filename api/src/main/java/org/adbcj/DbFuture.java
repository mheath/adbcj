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
package org.adbcj;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@code DbFuture} represents the result of an asynchronous database operation.  Methods are provided to check if
 * the operation is complete, to wait for its completion, to retrieve the result of the operation, and to register
 * {@link DbListener} objects that will be invoked when the operation completes.  The result is retrieved using methods
 * {@link #get()}, {@link #get(long, TimeUnit)}, or {@link #getUninterruptably()} when the operation has completed.
 * These methods will block if necessary until the operation completes.  Cancellation is performed with
 * {@link #cancel(boolean)}.  Additional methods are provided to determine if the operation completed normally or was
 * cancelled.  Once an operation has completed, it cannot be cancelled.  However, you may still use
 * {@link #addListener(DbListener)} when an operation has completed and the {@link DbListener#onCompletion(DbFuture)}
 * method will be invoked immediately.
 *
 * @author Mike Heath
 *
 * @param <T> The result type returned by this {@code DbFuture}'s {@code get()} methods
 */
public interface DbFuture<T> extends Future<T> {

	/**
	 * Adds a {@link DbListener} object the future.  The {@link DbListener#onCompletion(DbFuture)} method will be
	 * invoked when the database operation that this future represents completes.  If the operation as completed the
	 * <tt>onCompletion(HttpFuture)</tt> method will be invoked immediately.
	 *  
	 * @param listener  The listener that will be invoked when this future completes
	 * @return this <tt>DbFuture</tt> object.  Used for chained invocations.
	 */
	DbFuture<T> addListener(DbListener<T> listener);
	
	/**
	 * Removes a {@link DbListener} instance from the list of <tt>DbListener</tt>s that will be invoked when this
	 * future finishes.
	 * 
	 * @param listener  the listener that is to be removed.
	 * @return  true if the listener was successfully removed, false otherwise.
	 */
	boolean removeListener(DbListener<T> listener);
	
    /**
     * Waits if necessary for the database operation to complete, and then retrieves its result.
     *
     * @return the database operation result
     * @throws CancellationException if the operation was cancelled
     * @throws DbException if the operation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
	public T get() throws DbException, InterruptedException;
	
    /**
     * Waits if necessary for at most the given time for the database operation to complete, and then retrieves its
     * result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * 
     * @return the operation result
     * 
     * @throws CancellationException if the operation was cancelled
     * @throws DbException if the database operation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if the wait timed out
     */
	public T get(long timeout, TimeUnit unit) throws DbException, InterruptedException, TimeoutException;
	
    /**
     * Waits if necessary for the database operation to complete, and then retrieves its result without the possibility
     * of being interrupted.
     *
     * @return the operation result
     * 
     * @throws CancellationException if the operation was cancelled
     * @throws DbException if the operation threw an exception
     */
	public T getUninterruptably() throws DbException;
	
}
