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
package org.adbcj.tck;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.testng.annotations.DataProvider;

public class ConnectionManagerDataProvider {

	static {
		try {
			// Make sure static portions of each of these classes gets invoked
			org.adbcj.jdbc.Adbcj.init();
			org.adbcj.mysql.Adbcj.init();
			org.adbcj.postgresql.Adbcj.init();
			
			// Load the needed JDBC drivers
			Class.forName("com.mysql.jdbc.Driver");
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final Object[][] TEST_CONNECTION_DATA = {
		{"adbcj:jdbc:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:jdbc:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
		{"adbcj:postgresql://localhost/adbcjtck", "adbcjtck", "adbcjtck"},
	};
	
	@DataProvider(name="urlDataProvider")
	public static Object[][] urlDataProvider() {
		return TEST_CONNECTION_DATA;
	}
	
	@DataProvider(name="connectionManagerDataProvider")
	public static synchronized Object[][] connectionManagerProvider() {
		ConnectionManager[][] connectionManager = new ConnectionManager[TEST_CONNECTION_DATA.length][1];
		for (int i = 0 ; i < TEST_CONNECTION_DATA.length; i++) {
			Object[] connectParams = TEST_CONNECTION_DATA[i];
			connectionManager[i] = new ConnectionManager[] {ConnectionManagerProvider.createConnectionManager(
					connectParams[0].toString(),
					connectParams[1].toString(),
					connectParams[2].toString())
			};
		}
		return connectionManager;
	}

}
