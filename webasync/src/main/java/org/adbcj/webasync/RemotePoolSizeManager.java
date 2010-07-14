//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.webasync;

import org.adbcj.ConnectionPool;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class RemotePoolSizeManager {

	private ConnectionPool connectionPool;
	private Monitor monitor;

	public void update(int count, OutputStream out) throws Exception {
		connectionPool.setPoolSize(count);
		monitor.setCount(count);
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writer.write("Updated count to: " + count);
		writer.flush();
	}

	public void setConnectionPool(ConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}
}
