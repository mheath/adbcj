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
package net.toadhead.adbcj.jdbc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.ConnectionManagerProvider;
import edu.byu.cs.adbcj.ConnectionManagerProducer;
import edu.byu.cs.adbcj.DbException;

public class JdbcConnectionManagerProducer implements ConnectionManagerProducer {

	private static final String PROTOCOL = "jdbc";

	static {
		ConnectionManagerProvider.registerConnectionManagerProducer(PROTOCOL,
				new JdbcConnectionManagerProducer());

	}

	public ConnectionManager createConnectionManager(String url,
			String username, String password, ExecutorService executorService,
			Properties properties) throws DbException {
		try {
			URI uri = new URI(url);
			// Throw away the 'adbcj' protocol part of the URL
			uri = new URI(uri.getSchemeSpecificPart());

			String jdbcUrl = uri.toString();

			return new JdbcConnectionManager(jdbcUrl, username, password, executorService, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

}
