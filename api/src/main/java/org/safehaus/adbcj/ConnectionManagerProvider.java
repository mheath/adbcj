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
package org.safehaus.adbcj;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;


public class ConnectionManagerProvider {

	public static final String ADBCJ_PROTOCOL = "adbcj";

	private static final Map<String, ConnectionManagerFactory> CONNECTION_MANAGER_FACTORIES = new HashMap<String, ConnectionManagerFactory>();
	
	private ConnectionManagerProvider () {}
	
	public static ConnectionManager createConnectionManager(String url, String username, String password, ExecutorService executorService) throws DbException {
		return createConnectionManager(url, username, password, executorService, null);
	}
	
	public static ConnectionManager createConnectionManager(String url, String username, String password, ExecutorService executorService, Properties properties) throws DbException {
		if (url == null) {
			throw new IllegalArgumentException("Connection url can not be null");
		}
		
		try {
			URI uri = new URI(url);
			String adbcjProtocol = uri.getScheme();
			if (!ADBCJ_PROTOCOL.equals(adbcjProtocol)) {
				throw new DbException("Invalid connection URL: " + url);
			}
			URI driverUri = new URI(uri.getSchemeSpecificPart());
			String protocol = driverUri.getScheme();
			
			ConnectionManagerFactory factory = getConnectionManagerFactory(protocol, properties);
			return factory.createConnectionManager(url, username, password, executorService, properties);
		} catch (URISyntaxException e) {
			throw new DbException("Invalid connection URL: " + url);
		}
		
	}
	
	public static synchronized void registerConnectionManagerFactory(String protocol, ConnectionManagerFactory factory) {
		CONNECTION_MANAGER_FACTORIES.put(protocol, factory);
	}
	
	private static synchronized ConnectionManagerFactory getConnectionManagerFactory(String protocol, Properties properties) {
		ConnectionManagerFactory factory = CONNECTION_MANAGER_FACTORIES.get(protocol);
		if (factory == null) {
			throw new DbException(String.format("No adbcj driver registered with protocol '%s'", protocol));
		}
		return factory;
	}
	

}
