package org.adbcj.web;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.adbcj.DbSessionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfig;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Heath
 */
public class Jdbc implements HttpRequestHandler {

	private FreeMarkerConfig freeMarkerConfig;

	private List<SimpleJdbcTemplate> templates;

	private String query;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Configuration configuration = freeMarkerConfig.getConfiguration();
		configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
		final Template template = configuration.getTemplate("page.ftl");

		final Writer writer = response.getWriter();

		List<List<Map<String, Object>>> results = new ArrayList<List<Map<String, Object>>>();
		for (SimpleJdbcTemplate jdbcTemplate : templates) {
			results.add(jdbcTemplate.queryForList(query));
		}

		Map<String, Object> root = new HashMap<String, Object>();
		root.put("results", results);

		response.setContentType("text/html");
		try {
			template.process(root, response.getWriter());
		} catch (TemplateException e) {
			throw new ServletException(e);
		}
	}

	public void setFreeMarkerConfig(FreeMarkerConfig freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	public void setTemplates(List<SimpleJdbcTemplate> templates) {
		this.templates = templates;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}
