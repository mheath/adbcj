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

/**
 * Postgresql configuration variables.  See: http://developer.postgresql.org/pgdocs/postgres/runtime-config-client.html
 * 
 * @author Mike Heath
 *
 */
public enum ConfigurationVariable {
	SEARCH_PATH("search_path"),
	DEFAULT_TABLESPACE("default_tablespace"),
	TEMP_TABLESPACES("temp_tablespaces"),
	CHECK_FUNCTION_BODIES("check_function_bodies"), // boolean
	DEFAULT_TRANSACTION_ISOLATION("default_transaction_isolation"),
	DEFAULT_TRANSACTION_READ_ONLY("default_transaction_read_only"), // boolean
	STATEMENT_TIMEOUT("statement_timeout"), // integer
	SESSION_REPLICATION_ROLE("session_replication_role"),
	VACUUM_FREEZE_MIN_AGE("vacuum_freeze_min_age"), // integer
	XML_BINARY("xmlbinary"),
	XML_OPTION("xmloption"),
	DATE_STYLE("DateStyle"),
	TIMEZONE("timezone"),
	TIMEZONE_ABBREVIATIONS("timezone_abbreviations"),
	EXTRA_FLOAT_DIGITS("extra_float_digits"),
	CLIENT_ENCODING("client_encoding"),
	LC_MESSAGES("lc_messages"),
	LC_MONETARY("lc_monetary"),
	LC_NUMERIC("lc_numeric"),
	LC_TIME("lc_time"),
	DEFAULT_TEXT_SEARCH_CONFIG("default_text_search_config"),
	EXPLAIN_PRETTY_PRINT("explain_pretty_print"),
	DYNAMIC_LIBRARY_PATH("dynamic_library_path"),
	GIN_FUZZY_SEARCH_LIMIT("gin_fuzzy_search_limit"), // integer
	LOCAL_PRELOAD_LIBRARIES("local_preload_libraries"),
	INTEGER_DATETIMES("integer_datetimes"), // boolean
	IS_SUPERUSER("is_superuser"), // boolean
	SERVER_ENCODING("server_encoding"),
	SERVER_VERSION("server_version"),
	SESSION_AUTHORIZATION("session_authorization"),
	STANDARD_CONFORMING_STRINGS("standard_conforming_strings"),
	
	;
	
	private final String name;
	
	private ConfigurationVariable(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static ConfigurationVariable fromName(String name) {
		for (ConfigurationVariable cv : values()) {
			if (name.toUpperCase().equals(cv.name.toUpperCase())) {
				return cv;
			}
		}
		return null;
	}
}
