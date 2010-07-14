//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.web;

import org.adbcj.ConnectionPool;

import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class RemotePoolSizeManager implements HttpRequestHandler {

	private Map<String, ConnectionPool> pools;
	private Monitor monitor;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int poolSize = Integer.valueOf(request.getParameter("pool_size"));
		int count  = 0;
		for (ConnectionPool pool : pools.values()) {
			pool.setPoolSize(poolSize);
			count++;
		}

		monitor.setCount(poolSize);

		response.setContentType("text/plain");
		response.getWriter().write("Pool size set to " + poolSize + " for " + count + " pools.\n");
	}

	public void setPools(Map<String, ConnectionPool> pools) {
		this.pools = pools;
	}

	public void setMonitor(Monitor monitor) {
		this.monitor = monitor;
	}
}
