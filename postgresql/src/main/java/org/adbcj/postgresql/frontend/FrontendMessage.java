package org.adbcj.postgresql.frontend;

public class FrontendMessage extends AbstractFrontendMessage {

	public static final FrontendMessage FLUSH = new FrontendMessage(FrontendMessageType.FLUSH);
	public static final FrontendMessage SYNC = new FrontendMessage(FrontendMessageType.SYNC);
	public static final FrontendMessage TERMINATE = new FrontendMessage(FrontendMessageType.TERMINATE);
	
	private final FrontendMessageType type;
	private final byte[] data;

	public FrontendMessage(FrontendMessageType type) {
		this.type = type;
		this.data = null;
	}
	
	public FrontendMessage(FrontendMessageType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public FrontendMessageType getType() {
		return type;
	}
	
	public byte[] getData() {
		return data;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type.name());
		if (data != null) {
			builder.append(": ");
			for (int i = 0; i < data.length; i++) {
				byte b = data[i];
				builder.append(b).append("[0x").append(Integer.toHexString(b)).append("]");
				builder.append("[").append((char)b).append("]");
				if (i < data.length - 1) {
					builder.append(", ");
				}
			}
		}
		return builder.toString();
	}
	
}
