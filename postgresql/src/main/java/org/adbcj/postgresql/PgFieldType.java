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

public class PgFieldType {
	public static final int BOOLEAN = 16;
	public static final int BIGINT = 20;
	public static final int SMALLINT = 21;
	public static final int INTEGER = 23;
	public static final int REAL = 700;
	public static final int DOUBLE = 701;
	public static final int CHAR = 1042;
	public static final int VARCHAR = 1043;
	public static final int DATE = 1082;
	
	private PgFieldType() {}
}
