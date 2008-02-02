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

public class FrontendMessage extends AbstractFrontendMessage {

	public static final FrontendMessage FLUSH = new FrontendMessage(FrontendMessageType.FLUSH);
	public static final FrontendMessage SYNC = new FrontendMessage(FrontendMessageType.SYNC);
	public static final FrontendMessage TERMINATE = new FrontendMessage(FrontendMessageType.TERMINATE);
	
	private final FrontendMessageType type;
	private final byte[] data;

	public FrontendMessage(FrontendMessageType type) {
		this.type = type;
		this.data = null;
	}
	
	public FrontendMessage(FrontendMessageType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public FrontendMessageType getType() {
		return type;
	}
	
	public byte[] getData() {
		return data;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type.name());
		if (data != null) {
			builder.append(": ");
			for (int i = 0; i < data.length; i++) {
				byte b = data[i];
				builder.append(b).append("[0x").append(Integer.toHexString(b)).append("]");
				builder.append("[").append((char)b).append("]");
				if (i < data.length - 1) {
					builder.append(", ");
				}
			}
		}
		return builder.toString();
	}
	
}
