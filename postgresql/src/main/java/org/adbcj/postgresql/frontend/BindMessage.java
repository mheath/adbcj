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

import org.adbcj.postgresql.FormatCode;

public class BindMessage extends AbstractFrontendMessage {

	private final String portal;
	private final String statement;
	private final FormatCode[] parameterFormats;
	private final String[] parameterValues;
	private final FormatCode[] resultFormats;
	
	public BindMessage() {
		this(null, null, null, null, null);
	}
	
	public BindMessage(String statement) {
		this(statement, null, null, null, null);
	}
	
	public BindMessage(String statement, String portal, FormatCode[] parameterFormats, String[] parameterValues, FormatCode[] resultFormats) {
		this.statement = statement;
		this.portal = portal;
		this.parameterFormats = parameterFormats;
		this.parameterValues = parameterValues;
		this.resultFormats = resultFormats;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.BIND;
	}

	public String getPortal() {
		return portal;
	}

	public String getStatement() {
		return statement;
	}

	public FormatCode[] getParameterFormats() {
		return parameterFormats;
	}

	public String[] getParameterValues() {
		return parameterValues;
	}

	public FormatCode[] getResultFormats() {
		return resultFormats;
	}

}
