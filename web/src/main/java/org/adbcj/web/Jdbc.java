package org.adbcj.web;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
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
public class Jdbc implements HttpRequestHandler {

	private FreeMarkerConfig freeMarkerConfig;

	private Map<String, SimpleJdbcTemplate> templates;

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
		{
			String query = "select count(*) from access_log where time > " + time + " and time < " + (time + 10000) + ";";
			root.put("count1", templates.get("mysql_logs").queryForInt(query));
			root.put("count2", templates.get("pg_logs").queryForInt(query));
		}

		List<List<Map<String, Object>>> contacts1 = new ArrayList<List<Map<String, Object>>>();
		List<List<Map<String, Object>>> contacts2 = new ArrayList<List<Map<String, Object>>>();
		for (int i = 0; i < 5; i++) {
			String query = "select name, phone from contacts where id = " + random.nextInt(40000) + ";";
			contacts1.add(templates.get("mysql_contacts").queryForList(query));
			contacts2.add(templates.get("pg_contacts").queryForList(query));
		}
		root.put("contacts1", contacts1);
		root.put("contacts2", contacts2);

		response.setContentType("text/html");
		try {
			template.process(root, writer);
		} catch (TemplateException e) {
			throw new ServletException(e);
		}
	}

	public void setFreeMarkerConfig(FreeMarkerConfig freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	public void setTemplates(Map<String, SimpleJdbcTemplate> templates) {
		this.templates = templates;
	}

}
