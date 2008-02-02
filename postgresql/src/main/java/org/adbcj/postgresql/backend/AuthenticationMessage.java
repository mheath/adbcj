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

public class AuthenticationMessage extends AbstractBackendMessage {

	private final AuthenticationType authenticaitonType;
	private final byte[] data;
	
	public AuthenticationMessage(AuthenticationType authenticationType) {
		this.authenticaitonType = authenticationType;
		data = null;
	}
	
	public AuthenticationMessage(AuthenticationType authenticationType, byte[] data) {
		this.authenticaitonType = authenticationType;
		this.data = data;
	}

	@Override
	public BackendMessageType getType() {
		return BackendMessageType.AUTHENTICATION;
	}

	public AuthenticationType getAuthenticaitonType() {
		return authenticaitonType;
	}
	
	public byte[] getData() {
		return data;
	}
	
}
