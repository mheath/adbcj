package org.adbcj.mysql.netty;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

class MessageQueuingHandler implements ChannelUpstreamHandler {

	private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<ChannelHandlerContext>();
	// Access must be synchronized on this
	private final List<MessageEvent> messageQueue = new LinkedList<MessageEvent>();

	// Access must be synchronized on this
	private boolean flushed = false;

	@Override
	public synchronized void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if (!flushed) {
			if (e instanceof MessageEvent) {
				context.compareAndSet(null, ctx);
				messageQueue.add((MessageEvent) e);
			}
		}
	}

	public synchronized void flush() {
		for (MessageEvent event : messageQueue) {
			context.get().sendUpstream(event);
		}
		flushed = true;
	}

}
