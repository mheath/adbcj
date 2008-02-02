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
package org.adbcj.postgresql;

import java.util.Map;

import org.adbcj.DbException;

public class PgException extends DbException {
	private static final long serialVersionUID = 1L;

	private final Map<ErrorField, String> fields;
	
	public PgException(PgConnection connection, Map<ErrorField, String> fields) {
		super(connection);
		this.fields = fields;
	}

	public PgException(PgConnection connection, String message) {
		super(connection, message);
		this.fields = null;
	}

	public PgException(PgConnection connection, String message, Map<ErrorField, String> fields) {
		super(connection, message);
		this.fields = fields;
	}

	public Map<ErrorField, String> getFields() {
		return fields;
	}
	
	@Override
	public PgConnection getSession() {
		return (PgConnection)super.getSession();
	}
	
}
