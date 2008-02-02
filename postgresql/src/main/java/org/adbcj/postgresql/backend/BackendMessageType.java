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

public enum BackendMessageType {
	
	AUTHENTICATION((byte)'R'),
	BIND_COMPLETE((byte)'2'),
	CLOSE_COMPLETE((byte)'3'),
	COMMAND_COMPLETE((byte)'C'),
	COPY_DATA((byte)'c'),
	COPY_DONE((byte)'d'),
	COPY_IN_RESPONSE((byte)'G'),
	COPY_OUT_RESPONSE((byte)'H'),
	DATA_ROW((byte)'D'),
	EMPTY_QUERY_RESPONSE((byte)'I'),
	ERROR_RESPONSE((byte)'E'),
	FUNCTION_CALL_RESPONSE((byte)'V'),
	KEY((byte)'K'),
	NO_DATA((byte)'n'),
	NOTICE_RESPONSE((byte)'N'),
	NOTIFICATION_RESPONSE((byte)'A'),
	PARAMETER_DESCRIPTION((byte)'t'),
	PARAMETER_STATUS((byte)'S'),
	PARSE_COMPLETE((byte)'1'),
	PASSWORD((byte)'p'),
	PORTAL_SUSPENDED((byte)'s'),
	READY_FOR_QUERY((byte)'Z'),
	ROW_DESCRIPTION((byte)'T');
	
	private final byte value;
	
	private BackendMessageType(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	public static BackendMessageType fromValue(byte value) {
		for (BackendMessageType type : values()) {
			if (type.value == value) {
				return type;
			}
		}
		return null;
	}
}
