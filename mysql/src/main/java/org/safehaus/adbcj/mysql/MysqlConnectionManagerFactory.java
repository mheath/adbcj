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
package org.safehaus.adbcj.mysql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.ConnectionManagerFactory;
import org.safehaus.adbcj.ConnectionManagerProvider;
import org.safehaus.adbcj.DbException;

public class MysqlConnectionManagerFactory implements ConnectionManagerFactory {

	public static final String PROTOCOL = "mysql";
	public static final int DEFAULT_PORT = 3306;
	
	static void register() {
		ConnectionManagerProvider.registerConnectionManagerFactory(PROTOCOL, new MysqlConnectionManagerFactory());
	}
	
	
	private MysqlConnectionManagerFactory() {
	}
	
	public ConnectionManager createConnectionManager(String url, String username, String password, ExecutorService executorService, Properties properties) throws DbException {
		try {
			/*
			 * Parse URL
			 */
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			String host = uri.getHost();
			int port = uri.getPort();
			if (port < 0) {
				port = DEFAULT_PORT;
			}
			String schema = uri.getPath().substring(1);

			return new MysqlConnectionManager(host, port, username, password, schema, executorService, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

}
