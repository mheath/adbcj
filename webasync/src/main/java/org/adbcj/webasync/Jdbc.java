//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.webasync;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Jdbc  {

	private SimpleJdbcTemplate jdbcTemplate;

	private FreeMarkerConfigurationFactory freeMarkerConfig;

	public void render(OutputStream out) throws Exception {
		Configuration configuration = freeMarkerConfig.createConfiguration();
		configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
		final Template template = configuration.getTemplate("page.ftl");

		Map<String, Object> root = new HashMap<String, Object>();
		root.put("result", jdbcTemplate.queryForList("SELECT int_val as a, str_val as b FROM simple_values WHERE int_val is not null"));

		template.process(root, new OutputStreamWriter(out));
	}

	public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setFreeMarkerConfig(FreeMarkerConfigurationFactory freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}
}
