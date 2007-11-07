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
package org.safehaus.adbcj.support;

public abstract class Request<T> {
	
	private DefaultDbSessionFuture<T> future = null;
	private Object payload;

	public abstract void execute() throws Exception;
	
	public boolean cancel(boolean mayInterruptIfRunning) {
		return true;
	}
	
	public DefaultDbSessionFuture<T> getFuture() {
		return future;
	}
	
	public boolean canRemove() {
		return true;
	}

	public void setFuture(DefaultDbSessionFuture<T> future) {
		if (this.future != null) {
			throw new IllegalStateException("future can only be set once");
		}
		this.future = future;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

}
