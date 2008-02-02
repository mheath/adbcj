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

public enum ErrorField {
	SEVERITY('S', "Severity"),
	CODE('C', "Code"),
	MESSAGE('M', "Message"),
	DETAIL('D', "Detail"),
	HINT('H', "Hint"),
	POSITION('P', "Position"),
	INTERNAL_POSITION('p', "Internal Position"),
	INTERNAL_QUERY('q', "Internal query"),
	WHERE('W', "Where"),
	FILE('F', "File"),
	LINE('L', "Line"),
	ROUTINE('R', "Routine");
	
	private final byte token;
	private final String fieldName;
	
	private ErrorField(char token, String fieldName) {
		this.token = (byte)token;
		this.fieldName = fieldName;
	}
	
	public byte getToken() {
		return token;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public static ErrorField toErrorField(byte token) {
		for (ErrorField field : values()) {
			if (token == field.token) {
				return field;
			}
		}
		return null;
	}
}
