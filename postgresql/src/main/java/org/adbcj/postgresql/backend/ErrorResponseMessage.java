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
package org.adbcj.postgresql.backend;

import java.util.Collections;
import java.util.Map;

import org.adbcj.postgresql.ErrorField;

public class ErrorResponseMessage extends AbstractBackendMessage {

	private final Map<ErrorField, String> fields;
	
	public ErrorResponseMessage(Map<ErrorField, String> fields) {
		this.fields = Collections.unmodifiableMap(fields);
	}
	
	@Override
	public BackendMessageType getType() {
		return BackendMessageType.ERROR_RESPONSE;
	}
	
	public Map<ErrorField, String> getFields() {
		return fields;
	}

}
