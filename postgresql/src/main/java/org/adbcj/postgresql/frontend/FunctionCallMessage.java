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

public class FunctionCallMessage extends AbstractFrontendMessage {

	private final int objectId;
	private final FormatCode[] parameterFormats;
	private final String[] parameterValues;
	private final FormatCode[] resultFormats;

	public FunctionCallMessage(int objectId, FormatCode[] parameterFormats, String[] parameterValues, FormatCode[] resultFormats) {
		this.objectId = objectId;
		this.parameterFormats = parameterFormats;
		this.parameterValues = parameterValues;
		this.resultFormats = resultFormats;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.FUNCTION_CALL;
	}

	public int getObjectId() {
		return objectId;
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
