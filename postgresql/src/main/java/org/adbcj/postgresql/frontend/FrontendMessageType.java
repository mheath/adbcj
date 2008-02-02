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
package org.adbcj.postgresql.frontend;

public enum FrontendMessageType {
	BIND((byte)'B'),
	CANCEL_REQUEST((byte)0xFF),
	CLOSE((byte)'C'),
	COPY_DATA((byte)'c'),
	COPY_DONE((byte)'d'),
	COPY_FAIL((byte)'f'),
	DESCRIBE((byte)'D'),
	EXECUTE((byte)'E'),
	FLUSH((byte)'H'),
	FUNCTION_CALL((byte)'F'),
	PARSE((byte)'P'),
	PASSWORD((byte)'p'),
	QUERY((byte)'Q'),
	SSL((byte)0xFF),
	STARTUP((byte)0xFF),
	SYNC((byte)'S'),
	TERMINATE((byte)'X');
	
	private final byte value;
	
	private FrontendMessageType(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	public boolean includedInPacket() {
		return value != (byte)0xFF;
	}

}
