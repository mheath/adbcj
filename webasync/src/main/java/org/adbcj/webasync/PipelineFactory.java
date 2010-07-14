package org.adbcj.webasync;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Mike Heath
 */
public class PipelineFactory implements ChannelPipelineFactory {

	private ChannelHandler handler;

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("handler", handler);
		return pipeline;
	}

	@Required
	public void setHandler(ChannelHandler handler) {
		this.handler = handler;
	}
}