//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.web;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Hybrid implements HttpRequestHandler {

	private FreeMarkerConfig freeMarkerConfig;

	private Map<String, SimpleJdbcTemplate> templates;

	private ExecutorService executorService;

	private final AtomicLong queueTime = new AtomicLong();
	private final AtomicInteger queueCount = new AtomicInteger();
	private final AtomicLong queryTime = new AtomicLong();
	private final AtomicInteger queryCount = new AtomicInteger();
	private final AtomicLong totalTime = new AtomicLong();
	private final AtomicInteger timeCount = new AtomicInteger();

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Configuration configuration = freeMarkerConfig.getConfiguration();
		configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
		final Template template = configuration.getTemplate("page.ftl");

		final Writer writer = response.getWriter();

		final Random random = new Random();

		final Map<String, Object> root = new HashMap<String, Object>();

		final int maxTime = 1247052675;
		int time = random.nextInt(maxTime);
		final String query = "select count(*) from access_log where time > " + time + " and time < " + (time + 10000) + ";";
		final long start = System.currentTimeMillis();
		Future<Integer> count1Future = executorService.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				long queryStart = System.currentTimeMillis();
				queueTime.getAndAdd(queryStart - start);
				queueCount.incrementAndGet();
				int count = templates.get("mysql_logs").queryForInt(query);
				queryTime.getAndAdd(System.currentTimeMillis() - queryStart);
				queryCount.incrementAndGet();
				return count;
			}
		});
		Future<Integer> count2Future = executorService.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return templates.get("pg_logs").queryForInt(query);
			}
		});
		int[] numbers = {random.nextInt(40000), random.nextInt(40000), random.nextInt(40000), random.nextInt(40000), random.nextInt(40000)};
		Future<List<List<Map<String, Object>>>> contacts1Future = executorService.submit(new Select5Callable(templates.get("mysql_contacts"), numbers));
		Future<List<List<Map<String, Object>>>> contacts2Future = executorService.submit(new Select5Callable(templates.get("pg_contacts"), numbers));

		try {
			root.put("count1", count1Future.get());
			root.put("count2", count2Future.get());
			root.put("contacts1", contacts1Future.get());
			root.put("contacts2", contacts2Future.get());
			totalTime.getAndAdd(System.currentTimeMillis() - start);
			timeCount.incrementAndGet();

			response.setContentType("text/html");
			template.process(root, writer);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new ServletException(e);
		}

	}

	public void setFreeMarkerConfig(FreeMarkerConfig freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	public void setTemplates(Map<String, SimpleJdbcTemplate> templates) {
		this.templates = templates;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	private class Select5Callable implements Callable<List<List<Map<String, Object>>>> {

		private final SimpleJdbcTemplate jdbcTemplate;
		private final int[] numbers;

		private Select5Callable(SimpleJdbcTemplate jdbcTemplate, int[] numbers) {
			this.jdbcTemplate = jdbcTemplate;
			this.numbers = numbers;
		}

		@Override
		public List<List<Map<String, Object>>> call() throws Exception {
			List<List<Map<String, Object>>> contacts = new ArrayList<List<Map<String, Object>>>();
			for (int i : numbers) {
				String query = "select name, phone from contacts where id = " + i + ";";
				contacts.add(jdbcTemplate.queryForList(query));
			}
			return contacts;
		}
	}

	public void dump() {
		System.out.println("Average queue time: " + ((double)queueTime.get() / queueCount.get()));
		System.out.println("Average query time: " + ((double)queryTime.get() / queryCount.get()));
		System.out.println("Average total time: " + ((double)totalTime.get() / timeCount.get()));

		queueTime.set(0);
		queryCount.set(0);
		queryTime.set(0);
		queryCount.set(0);
		totalTime.set(0);
		timeCount.set(0);
	}

}
