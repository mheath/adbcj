//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.webasync;

import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import org.adbcj.ConnectionPool;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSession;
import org.adbcj.ResultSet;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Adbcj {

	private ConnectionPool connectionPool;

	private FreeMarkerConfigurationFactory freeMarkerConfig;

	public void render(final ChannelHandlerContext ctx, final HttpRequest request) throws Exception {
		Configuration configuration = freeMarkerConfig.createConfiguration();
		configuration.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
		final Template template = configuration.getTemplate("page.ftl");

		final DbSession session = connectionPool.connect().get();
		session.executeQuery("SELECT int_val as a, str_val as b FROM simple_values WHERE int_val is not null").addListener(new DbListener<ResultSet>() {
			@Override
			public void onCompletion(DbFuture<ResultSet> resultSetFuture) throws Exception {
				// Render page
				ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(4096);
				OutputStream out = new ChannelBufferOutputStream(buffer);
				Map<String, Object> root = new HashMap<String, Object>();
				root.put("result", resultSetFuture.get());
				template.process(root, new OutputStreamWriter(out));

				// Create response
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
				response.setContent(buffer);
				response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());

				// Close channel if necessary
				ChannelFuture f = ctx.getChannel().write(response);
				if (!HttpHeaders.isKeepAlive(request) || response.getStatus().getCode() != 200) {
					f.addListener(ChannelFutureListener.CLOSE);
				}

				session.close(true);
			}
		});
	}

	public void setConnectionPool(ConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public void setFreeMarkerConfig(FreeMarkerConfigurationFactory freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}
}
