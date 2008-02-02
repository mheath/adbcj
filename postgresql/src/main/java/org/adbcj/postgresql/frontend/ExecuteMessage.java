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

public class ExecuteMessage extends AbstractFrontendMessage {

	private final String portal;
	private final int maxRows;
	
	public ExecuteMessage() {
		this(null, 0);
	}
	
	public ExecuteMessage(int maxRows) {
		this(null, maxRows);
	}
	
	public ExecuteMessage(String portal, int maxRows) {
		this.portal = portal;
		this.maxRows = maxRows;
	}

	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.EXECUTE;
	}

	public String getPortal() {
		return portal;
	}

	public int getMaxRows() {
		return maxRows;
	}

}
