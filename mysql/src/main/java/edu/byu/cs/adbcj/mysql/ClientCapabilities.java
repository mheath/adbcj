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
package edu.byu.cs.adbcj.mysql;

public enum ClientCapabilities {
	LONG_PASSWORD,
	FOUND_ROWS,
	LONG_COLUMN_FLAG,
	CONNECT_WITH_DB,
	NO_SCHEMA,
	COMPRESS,
	ODBC_CLIENT,
	LOCAL_FILES,
	IGNORE_SPACES,
	PROTOCOL_4_1,
	INTERACTIVE,
	SSL,
	IGNORE_SIGPIPE,
	TRANSACTIONS,
	PROTOCOL_4_1_OLD,
	SECURE_AUTHENTICATION,
}
