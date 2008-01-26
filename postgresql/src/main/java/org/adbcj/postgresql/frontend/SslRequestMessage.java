package org.adbcj.postgresql.frontend;

public class SslRequestMessage extends AbstractFrontendMessage {

	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.SSL;
	}

}
