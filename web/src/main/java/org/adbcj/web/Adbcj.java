package org.adbcj.web;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbSession;
import org.adbcj.DbSessionFuture;
import org.adbcj.DbSessionPool;
import org.adbcj.ResultSet;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mike Heath
 */
public class Adbcj implements HttpRequestHandler {

	private FreeMarkerConfig freeMarkerConfig;

	private List<DbSessionPool> pools;

	private String query;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			Configuration configuration = freeMarkerConfig.getConfiguration();
			configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
			final Template template = configuration.getTemplate("page.ftl");

			final Writer writer = response.getWriter();

			final CountDownLatch latch = new CountDownLatch(1);

			// Fork out requests
			List<DbFuture<ResultSet>> futures = new ArrayList<DbFuture<ResultSet>>();
			for (DbSessionPool sessionPool : pools) {
				// We know this will return immediately without blocking
				DbSession session = sessionPool.connect().getUninterruptably();
				DbSessionFuture<ResultSet> future = session.executeQuery(query);
				futures.add(future);

			}

			// Join results
			List<ResultSet> results = null;
			try {
				results = new ArrayList<ResultSet>();
				for (DbFuture<ResultSet> future : futures) {
					results.add(future.get());
				}
			} catch (InterruptedException e) {
				throw new ServletException(e);
			}

			Map<String, Object> root = new HashMap<String, Object>();
			root.put("results", results);

			response.setContentType("text/html");
			try {
				template.process(root, response.getWriter());
			} catch (TemplateException e) {
				throw new ServletException(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
	}

	public void setFreeMarkerConfig(FreeMarkerConfig freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	public void setPools(List<DbSessionPool> pools) {
		this.pools = pools;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}
