package org.adbcj.web;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.adbcj.ConnectionPool;
import org.adbcj.DbSession;
import org.adbcj.DbSessionFuture;
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
import java.util.Random;

/**
 * @author Mike Heath
 */
public class Adbcj implements HttpRequestHandler {

	private FreeMarkerConfig freeMarkerConfig;

	private Map<String, ConnectionPool> pools;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			Configuration configuration = freeMarkerConfig.getConfiguration();
			configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
			final Template template = configuration.getTemplate("page.ftl");

			final Writer writer = response.getWriter();

			final Random random = new Random();

			DbSession mysqlContactsSession = pools.get("mysql_contacts").connect().getUninterruptably();
			DbSession mysqlLogsSession = pools.get("mysql_logs").connect().getUninterruptably();
			DbSession pgContactsSession = pools.get("pg_contacts").connect().getUninterruptably();
			DbSession pgLogsSession = pools.get("pg_logs").connect().getUninterruptably();

			// Fetch contacts
			List<DbSessionFuture<ResultSet>> contacts1 = new ArrayList<DbSessionFuture<ResultSet>>();
			List<DbSessionFuture<ResultSet>> contacts2 = new ArrayList<DbSessionFuture<ResultSet>>();
			for (int i = 0; i < 5; i++) {
				String query = "select name, phone from contacts where id = " + random.nextInt(40000) + ";";
				contacts1.add(mysqlContactsSession.executeQuery(query));
				contacts2.add(pgContactsSession.executeQuery(query));
			}

			// Fetch log counts
			final int maxTime = 1247052675;
			int time = random.nextInt(maxTime);
			String query = "select count(*) from access_log where time > " + time + " and time < " + (time + 10000) + ";";
			DbSessionFuture<ResultSet> log1Future = mysqlLogsSession.executeQuery(query);
			DbSessionFuture<ResultSet> log2Future = pgLogsSession.executeQuery(query);

			mysqlContactsSession.close(true);
			mysqlLogsSession.close(true);
			pgContactsSession.close(true);
			pgLogsSession.close(true);
			
			// Add contacts to freemarker context
			Map<String, Object> root = new HashMap<String, Object>();
			root.put("contacts1", compileContacts(contacts1));
			root.put("contacts2", compileContacts(contacts2));

			// Add log counts to freemarker context
			root.put("count1", log1Future.getUninterruptably().get(0).get(0));
			root.put("count2", log2Future.getUninterruptably().get(0).get(0));

			response.setContentType("text/html");
			try {
				template.process(root, writer);
			} catch (TemplateException e) {
				throw new ServletException(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}
	}

	private List<ResultSet> compileContacts(List<DbSessionFuture<ResultSet>> contacts) {
		List<ResultSet> results = new ArrayList<ResultSet>();
		for (DbSessionFuture<ResultSet> future : contacts) {
			results.add(future.getUninterruptably());
		}
		return results;
	}

	public void setFreeMarkerConfig(FreeMarkerConfig freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	public void setPools(Map<String, ConnectionPool> pools) {
		this.pools = pools;
	}
}
