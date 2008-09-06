package org.adbcj.mysql.netty;

import org.adbcj.Connection;
import org.adbcj.mysql.codec.AbstractMySqlConnection;
import org.adbcj.mysql.codec.AbstractMySqlConnectionManager;
import org.adbcj.mysql.codec.ClientRequest;
import org.adbcj.mysql.codec.LoginCredentials;
import org.adbcj.support.DefaultDbFuture;
import org.jboss.netty.channel.Channel;

public class MysqlConnection extends AbstractMySqlConnection {

	private final Channel channel;
	private final DefaultDbFuture<Connection> connectFuture;

	public MysqlConnection(AbstractMySqlConnectionManager connectionManager, LoginCredentials credentials, Channel channel, DefaultDbFuture<Connection> connectFuture) {
		super(connectionManager, credentials);
		this.channel = channel;
		this.connectFuture = connectFuture;
	}

	@Override
	public DefaultDbFuture<Connection> getConnectFuture() {
		return connectFuture;
	}

	@Override
	protected boolean isTransportClosing() {
		return !channel.isOpen();
	}

	@Override
	public void write(ClientRequest request) {
		channel.write(request);
	}

}
