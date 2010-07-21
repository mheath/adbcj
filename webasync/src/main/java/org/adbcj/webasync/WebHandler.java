//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.webasync;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import java.io.OutputStream;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class WebHandler extends SimpleChannelUpstreamHandler {

	private Jdbc jdbc;

	private Adbcj adbcj;

	private Hybrid hybrid;

	private RemotePoolSizeManager remotePoolSizeManager;

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof HttpRequest) {
			handleHttpRequest(ctx, (HttpRequest) msg);
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
		try {
			if ("/".equals(request.getUri())) {
				sendPlainResponse(HttpResponseStatus.OK, "Event Driven Web Server Test".getBytes(), ctx, request);
			} else if ("/jdbc".equals(request.getUri())) {
				ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(4096);
				OutputStream out = new ChannelBufferOutputStream(buffer);
				jdbc.render(out);
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
				response.setContent(buffer);
				response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
				sendHttpResponse(ctx, request, response);
			} else if ("/adbcj".equals(request.getUri())) {
				adbcj.render(ctx, request);
			} else if ("/hybrid".equals(request.getUri())) {
				hybrid.render(ctx, request);
			} else if (request.getUri().startsWith("/count/")) {
				int count = Integer.parseInt(request.getUri().substring(7));
				ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(4096);
				OutputStream out = new ChannelBufferOutputStream(buffer);
				remotePoolSizeManager.update(count, out);
				HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
				response.setContent(buffer);
				response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
				sendHttpResponse(ctx, request, response);
			} else {
				sendPlainResponse(HttpResponseStatus.NOT_FOUND, "Page not found".getBytes(), ctx, request);
			}
		} catch (Throwable t) {
			byte[] content = t.getClass().getName().getBytes();
			sendPlainResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, content, ctx, request);
			t.printStackTrace();
		}
	}

	private void sendPlainResponse(HttpResponseStatus status, byte[] content, ChannelHandlerContext ctx, HttpRequest request) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
		response.setContent(ChannelBuffers.wrappedBuffer(content));
		response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, content.length);
		sendHttpResponse(ctx, request, response);
	}

	private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response) {
		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.getChannel().write(response);
		if (!HttpHeaders.isKeepAlive(request) || response.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void setJdbc(Jdbc jdbc) {
		this.jdbc = jdbc;
	}

	public void setAdbcj(Adbcj adbcj) {
		this.adbcj = adbcj;
	}

	public void setHybrid(Hybrid hybrid) {
		this.hybrid = hybrid;
	}

	public void setRemotePoolSizeManager(RemotePoolSizeManager remotePoolSizeManager) {
		this.remotePoolSizeManager = remotePoolSizeManager;
	}
}
