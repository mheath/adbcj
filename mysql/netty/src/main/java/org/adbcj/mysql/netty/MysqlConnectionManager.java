package org.adbcj.mysql.netty;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.adbcj.Connection;
import org.adbcj.mysql.codec.AbstractMySqlConnectionManager;
import org.adbcj.mysql.codec.ClientRequest;
import org.adbcj.mysql.codec.MySqlClientDecoder;
import org.adbcj.mysql.codec.MySqlClientEncoder;
import org.adbcj.mysql.codec.ProtocolHandler;
import org.adbcj.support.DefaultDbFuture;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlConnectionManager extends AbstractMySqlConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(MysqlConnectionManager.class);

	private static final String QUEUE_HANDLER = MysqlConnectionManager.class.getName() + ".queueHandler";
	private static final String ENCODER = MysqlConnectionManager.class.getName() + ".encoder";
	private static final String DECODER = MysqlConnectionManager.class.getName() + ".decoder";

	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private final ClientBootstrap bootstrap;

	public MysqlConnectionManager(String host, int port, String username, String password, String schema, Properties properties) {
		super(username, password, schema, properties);

		ChannelFactory factory = new NioClientSocketChannelFactory(executorService, executorService);
		bootstrap = new ClientBootstrap(factory);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();

				pipeline.addFirst(QUEUE_HANDLER, new MessageQueuingHandler());

				pipeline.addLast(DECODER, new Decoder());
				pipeline.addLast(ENCODER, new Encoder());

				return pipeline;
			}
		});
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
		bootstrap.setOption("remoteAddress", new InetSocketAddress(host, port));
	}

	@Override
	protected void dispose() {
		executorService.shutdownNow();
	}

	@Override
	protected DefaultDbFuture<Connection> createConnectionFuture() {
		final ChannelFuture channelFuture = bootstrap.connect();
		return new MysqlConnectFuture(channelFuture);
	}

	class MysqlConnectFuture extends DefaultDbFuture<Connection> {
		private final ChannelFuture channelFuture;

		public MysqlConnectFuture(ChannelFuture channelFuture) {
			this.channelFuture = channelFuture;
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					Channel channel = future.getChannel();
					MysqlConnection connection = new MysqlConnection(MysqlConnectionManager.this, getCredentials(), channel, MysqlConnectFuture.this);
					channel.getPipeline().addLast("handler", new Handler(connection));
					channel.getPipeline().get(MessageQueuingHandler.class).flush();
					channel.getPipeline().remove(MessageQueuingHandler.class);

				}
			});
		}

		@Override
		protected boolean doCancel(boolean mayInterruptIfRunning) {
			return channelFuture.cancel();
		}
	}
}

@ChannelPipelineCoverage("one")
class Decoder extends FrameDecoder {

	private final MySqlClientDecoder decoder = new MySqlClientDecoder();

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		 InputStream in = new ChannelBufferInputStream(buffer);
		 try {
			 return decoder.decode(in, false);
		 } finally {
			 in.close();
		 }
	}

}

@ChannelPipelineCoverage("all")
class Encoder implements ChannelDownstreamHandler {

	private final MySqlClientEncoder encoder = new MySqlClientEncoder();

	public void handleDownstream(ChannelHandlerContext context, ChannelEvent event) throws Exception {
        if (!(event instanceof MessageEvent)) {
            context.sendDownstream(event);
            return;
        }

        MessageEvent e = (MessageEvent) event;
        if (!(e.getMessage() instanceof ClientRequest)) {
            context.sendDownstream(event);
            return;
        }

        ChannelBuffer buffer = ChannelBuffers.buffer(1024);
        ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
    	encoder.encode((ClientRequest) e.getMessage(), out);
    	Channels.write(context, e.getChannel(), e.getFuture(), buffer);
	}
}

@ChannelPipelineCoverage("one")
class Handler extends SimpleChannelHandler {

	private final MysqlConnection connection;
	private final ProtocolHandler handler = new ProtocolHandler();

	public Handler(MysqlConnection connection) {
		this.connection = connection;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		handler.messageReceived(connection, e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Throwable t = handler.handleException(connection, e.getCause());
		if (t != null) {
			// TODO: Pass exception on to connectionManager
			t.printStackTrace();
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		handler.connectionClosed(connection);
	}

}