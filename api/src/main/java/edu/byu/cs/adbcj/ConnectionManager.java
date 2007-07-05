package edu.byu.cs.adbcj;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConnectionManager {
	
	public static final String ADBCJ_PROTOCOL = "adbcj";
	
	private static Map<String, ConnectionFactory> connectionFactories = new HashMap<String, ConnectionFactory>();
	
	private ConnectionManager() {}
	
	public static DbFuture<Connection> connect(String url, String username, String password) {
		return connect(url, username, password, null);
	}
	
	public static DbFuture<Connection> connect(String url, String username, String password, Properties properties) {
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
			
			ConnectionFactory factory = getConnectionFactory(protocol, properties);
			return factory.connect(url, username, password, properties);
		} catch (URISyntaxException e) {
			throw new DbException("Invalid connection URL: " + url);
		}
	}

	public static synchronized void registerConnectionFactory(String protocol, ConnectionFactory factory) {
		connectionFactories.put(protocol, factory);
	}
	
	private static synchronized ConnectionFactory getConnectionFactory(String protocol, Properties properties) {
		ConnectionFactory factory = connectionFactories.get(protocol);
		if (factory == null) {
			throw new DbException(String.format("No driver connection factory registered with protocol '%s'", protocol));
		}
		return factory;
	}
	
}
