package org.adbcj.postgresql.netty;

import org.adbcj.DbFuture;
import org.adbcj.DbException;
import org.adbcj.support.DecoderInputStream;
import org.adbcj.support.DefaultDbFuture;
import org.adbcj.postgresql.codec.AbstractConnectionManager;
import org.adbcj.postgresql.codec.ConnectionState;
import org.adbcj.postgresql.codec.AbstractConnection;
import org.adbcj.postgresql.codec.ProtocolHandler;
import org.adbcj.postgresql.codec.backend.BackendMessageDecoder;
import org.adbcj.postgresql.codec.backend.AbstractBackendMessage;
import org.adbcj.postgresql.codec.frontend.FrontendMessageEncoder;
import org.adbcj.postgresql.codec.frontend.AbstractFrontendMessage;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;
import java.io.InputStream;

/**
 * @author Mike Heath
 */
public class NettyConnectionManager extends AbstractConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(NettyConnectionManager.class);

	private static final String QUEUE_HANDLER = NettyConnectionManager.class.getName() + ".queueHandler";
	private static final String ENCODER = NettyConnectionManager.class.getName() + ".encoder";
	private static final String DECODER = NettyConnectionManager.class.getName() + ".decoder";

	private final ExecutorService executorService;
	private final ClientBootstrap bootstrap;

	// Access must be synchronized on 'this'
	private DefaultDbFuture<Void> closeFuture = null;

	private volatile boolean pipeliningEnabled = true;

	public NettyConnectionManager(String host, int port, String username, String password, String database, Properties properties, ChannelFactory channelFactory) {
		super(username, password, database);
		executorService = null;
		bootstrap = initBootstrap(channelFactory, host, port);
	}

	public NettyConnectionManager(String host, int port, String username, String password, String database, Properties properties) {
		super(username, password, database);
		executorService = Executors.newCachedThreadPool();
		ChannelFactory channelFactory = new NioClientSocketChannelFactory(executorService, executorService);
		bootstrap = initBootstrap(channelFactory, host, port);
	}

	private ClientBootstrap initBootstrap(ChannelFactory channelFactory, String host, int port) {
		ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);

		bootstrap.setPipelineFactory(Channels.pipelineFactory(Channels.pipeline()));
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
		bootstrap.setOption("remoteAddress", new InetSocketAddress(host, port));

		return bootstrap;
	}


	@Override
	public DbFuture<org.adbcj.Connection> connect() {
		if (isClosed()) {
			throw new DbException("Connection manager is closed");
		}
		final ChannelFuture channelFuture = bootstrap.connect();
		return new PostgresqlConnectFuture(channelFuture);
	}

	@Override
	public DbFuture<Void> close(boolean immediate) throws DbException {
		if (isClosed()) {
			return closeFuture;
		}
		synchronized (this) {
			closeFuture = new DefaultDbFuture<Void>();
			if (immediate) {
				// TODO Shut down all connections managed by this ConnectionManager
				if (executorService != null) {
					executorService.shutdownNow();
				}
				closeFuture.setResult(null);
			} else {
				// TODO Implement NettyConnectionManager.close(boolean)
				throw new Error("Non immediate close not yet implemented");
			}
			return closeFuture;

		}
	}

	@Override
	public boolean isClosed() {
		synchronized (this) {
			return closeFuture != null;
		}
	}

	@Override
	public boolean isPipeliningEnabled() {
		return pipeliningEnabled;
	}

	@Override
	public void setPipeliningEnabled(boolean pipeliningEnabled) {
		this.pipeliningEnabled = pipeliningEnabled;
	}

	private class PostgresqlConnectFuture extends DefaultDbFuture<org.adbcj.Connection> {

		private final ChannelFuture channelFuture;

		public PostgresqlConnectFuture(ChannelFuture channelFuture) {
			this.channelFuture = channelFuture;
			channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					Channel channel = future.getChannel();
					Connection connection = new Connection(NettyConnectionManager.this, channel, PostgresqlConnectFuture.this);
					ConnectionState state = connection.getConnectionState();
					ProtocolHandler protocolHandler = new ProtocolHandler(NettyConnectionManager.this);

					ChannelPipeline pipeline = channel.getPipeline();
					pipeline.addLast(ENCODER, new Encoder(state));
					pipeline.addLast(DECODER, new Decoder(state));
					pipeline.addLast(QUEUE_HANDLER, new Handler(connection, protocolHandler));

					protocolHandler.connectionOpened(connection);
				}
			});
		}

		@Override
		protected boolean doCancel(boolean mayInterruptIfRunning) {
			return channelFuture.cancel();
		}

	}

}

class Connection extends AbstractConnection {

	private final Channel channel;
	private final DefaultDbFuture<org.adbcj.Connection> connectFuture;

	Connection(AbstractConnectionManager connectionManager, Channel channel, DefaultDbFuture<org.adbcj.Connection> connectFuture) {
		super(connectionManager);
		this.channel = channel;
		this.connectFuture = connectFuture;
	}

	@Override
	public DefaultDbFuture<org.adbcj.Connection> getConnectFuture() {
		return connectFuture;
	}

	@Override
	protected boolean isConnectionClosing() {
		return !channel.isOpen();
	}

	@Override
	protected void write(AbstractFrontendMessage message) {
		channel.write(message);
	}

	@Override
	protected void write(AbstractFrontendMessage[] messages) {
		channel.write(messages);
	}
}

@ChannelPipelineCoverage("one")
class Handler extends SimpleChannelHandler {
	private final Connection connection;
	private final ProtocolHandler protocolHandler;

	Handler(Connection connection, ProtocolHandler protocolHandler) {
		this.connection = connection;
		this.protocolHandler = protocolHandler;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		protocolHandler.handleMessage(connection, (AbstractBackendMessage) e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		protocolHandler.handleException(connection, e.getCause());
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		protocolHandler.closeConnection(connection);
	}


}

@ChannelPipelineCoverage("all")
class Decoder extends FrameDecoder {

	private final BackendMessageDecoder decoder;

	Decoder(ConnectionState state) {
		this.decoder = new BackendMessageDecoder(state);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		 InputStream in = new ChannelBufferInputStream(buffer);
		 DecoderInputStream dis = new DecoderInputStream(in);
		 try {
			 return decoder.decode(dis, false);
		 } finally {
			 dis.close();
		 }
	}

}

@ChannelPipelineCoverage("all")
class Encoder implements ChannelDownstreamHandler {

	private final FrontendMessageEncoder encoder;

	Encoder(ConnectionState state) {
		encoder = new FrontendMessageEncoder(state);
	}

	public void handleDownstream(ChannelHandlerContext context, ChannelEvent event) throws Exception {
        if (!(event instanceof MessageEvent)) {
            context.sendDownstream(event);
            return;
        }

        MessageEvent e = (MessageEvent) event;
		Object message = e.getMessage();
		boolean singleMessage = message instanceof AbstractFrontendMessage;
		boolean multipleMessages = message instanceof AbstractFrontendMessage[];
		if (!singleMessage && !multipleMessages) {
            context.sendDownstream(event);
            return;
        }

        ChannelBuffer buffer = ChannelBuffers.buffer(1024);
        ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
		if (singleMessage) {
			encoder.encode(out, (AbstractFrontendMessage) e.getMessage());
		} else {
			encoder.encode(out, (AbstractFrontendMessage[]) e.getMessage());
		}
    	Channels.write(context, e.getFuture(), buffer);
	}
}

