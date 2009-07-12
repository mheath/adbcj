package org.adbcj.postgresql.netty;

import org.adbcj.ConnectionManager;
import org.adbcj.DbException;
import org.adbcj.ConnectionManagerFactory;

import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Mike Heath
 */
public class NettyConnectionManagerFactory implements ConnectionManagerFactory {

	public static final String PROTOCOL = "postgresql-netty";
	public static final int DEFAULT_PORT = 5432;

	public ConnectionManager createConnectionManager(String url, String username, String password, Properties properties) throws DbException {
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

			return new NettyConnectionManager(host, port, username, password, schema, properties);
		} catch (URISyntaxException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean canHandle(String protocol) {
		return PROTOCOL.equals(protocol);
	}

}
