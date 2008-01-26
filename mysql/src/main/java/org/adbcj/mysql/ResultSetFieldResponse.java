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
package org.adbcj.mysql;


public class ResultSetFieldResponse extends Response {

	private final MysqlField field;
	
	public ResultSetFieldResponse(int packetLength, byte packetNumber, MysqlField field) {
		super(packetLength, packetNumber);
		this.field = field;
	}
	
	public MysqlField getField() {
		return field;
	}

}
