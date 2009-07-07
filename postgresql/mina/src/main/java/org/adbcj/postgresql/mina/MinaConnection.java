package org.adbcj.postgresql.mina;

import org.adbcj.postgresql.codec.AbstractConnection;
import org.adbcj.postgresql.codec.AbstractConnectionManager;
import org.adbcj.postgresql.codec.ConnectionState;
import org.adbcj.postgresql.codec.frontend.AbstractFrontendMessage;
import org.apache.mina.core.session.IoSession;

/**
 * @author Mike Heath
 */
public class MinaConnection extends AbstractConnection {

	private final MinaConnectionManager.PgConnectFuture connectFuture;
	private final IoSession session;

	public MinaConnection(AbstractConnectionManager connectionManager, MinaConnectionManager.PgConnectFuture connectFuture, IoSession session) {
		super(connectionManager);
		this.connectFuture = connectFuture;
		this.session = session;
	}

	public MinaConnectionManager.PgConnectFuture getConnectFuture() {
		return connectFuture;
	}

	@Override
	protected boolean isConnectionClosing() {
		return session.isClosing();
	}

	@Override
	protected void write(AbstractFrontendMessage message) {
		session.write(message);
	}

	@Override
	protected void write(AbstractFrontendMessage[] messages) {
		session.write(messages);
	}

}
