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
package org.safehaus.adbcj;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author Mike Heath
 *
 * @param <T>  The results type returned by this future's {@link #get()} method.
 */
public interface DbFuture<T> extends Future<T> {

	DbFuture<T> addListener(DbListener<T> listener);
	
	boolean removeListener(DbListener<T> listener);
	
	public T get() throws DbException, InterruptedException;
	
	public T get(long timeout, TimeUnit unit) throws DbException, InterruptedException, TimeoutException;
	
	public T getUninterruptably() throws DbException;
	
}
