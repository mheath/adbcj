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

public class ParseMessage extends AbstractFrontendMessage {

	private final String query;
	private final String statement;
	private final int[] parameters; // TODO The param types may need to be enums, figure this out
	
	public ParseMessage(String query) {
		this(query, null, null);
	}
	
	public ParseMessage(String query, String statement) {
		this(query, statement, null);
	}
	
	public ParseMessage(String query, String statement, int[] parameters) {
		this.query = query;
		this.statement = statement;
		this.parameters = parameters;
	}

	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.PARSE;
	}
	
	public int[] getParameters() {
		return parameters;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String getStatement() {
		return statement;
	}

}
