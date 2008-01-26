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

public class DbException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final DbSession session;
	
	public static DbException wrap(DbSession session, Throwable cause) {
		if (cause instanceof DbException) {
			return(DbException)cause;
		} else {
			return new DbException(session, cause);
		}
	}
	
	public DbException(String message) {
		super(message);
		session = null;
	}
	
	public DbException(Throwable cause) {
		super(cause);
		session = null;
	}
	
	public DbException(DbSession session) {
		super();
		this.session = session;
	}

	public DbException(DbSession session, String message, Throwable cause) {
		super(message, cause);
		this.session = session;
	}

	public DbException(DbSession session, String message) {
		super(message);
		this.session = session;
	}

	public DbException(DbSession session, Throwable cause) {
		super(cause);
		this.session = session;
	}

	public DbSession getSession() {
		return session;
	}
	
}
