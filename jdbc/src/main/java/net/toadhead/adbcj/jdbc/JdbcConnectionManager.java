package net.toadhead.adbcj.jdbc;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.support.ConcurrentFutureProxy;


public class JdbcConnectionManager implements ConnectionManager {

	private static final Object USER = "user";
	private static final Object PASSWORD = "password";
	
	private final String jdbcUrl;
	private final Properties properties;
	private final ExecutorService executorService;
	
	public JdbcConnectionManager(String jdbcUrl, String username,
			String password, ExecutorService executorService,
			Properties properties) {
		this.jdbcUrl = jdbcUrl;
		this.properties = new Properties(properties);
		this.executorService = executorService;
		
		this.properties.put(USER, username);
		this.properties.put(PASSWORD, password);
	}

	public DbFuture<Connection> connect() {
		final ConcurrentFutureProxy<Connection> future = new ConcurrentFutureProxy<Connection>();
		executorService.submit(new Callable<Connection>() {
			public Connection call() throws Exception {
				java.sql.Connection jdbcConnection = DriverManager.getConnection(jdbcUrl, properties);
				
				JdbcConnection connection = new JdbcConnection(JdbcConnectionManager.this, jdbcConnection);
				executorService.submit(new Runnable() {
					public void run() {
						future.setDone();
					}
				});
				return connection;
			}
		});
		return future;
	}

	public DbSessionFuture<Void> close(boolean immediate) throws DbException {
		// TODO Implement me
		throw new IllegalStateException("Not implemented");
	}

	public boolean isClosed() {
		// TODO Implement me
		throw new IllegalStateException("Not implemented");
	}

	/*
	 * 
	 * Non API Method
	 * 
	 */
	
	public ExecutorService getExecutorService() {
		return executorService;
	}

}
