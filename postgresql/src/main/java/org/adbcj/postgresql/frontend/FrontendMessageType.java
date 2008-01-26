package org.adbcj.postgresql.frontend;

public enum FrontendMessageType {
	BIND((byte)'B'),
	CANCEL_REQUEST((byte)0xFF),
	CLOSE((byte)'C'),
	COPY_DATA((byte)'c'),
	COPY_DONE((byte)'d'),
	COPY_FAIL((byte)'f'),
	DESCRIBE((byte)'D'),
	EXECUTE((byte)'E'),
	FLUSH((byte)'H'),
	FUNCTION_CALL((byte)'F'),
	PARSE((byte)'P'),
	PASSWORD((byte)'p'),
	QUERY((byte)'Q'),
	SSL((byte)0xFF),
	STARTUP((byte)0xFF),
	SYNC((byte)'S'),
	TERMINATE((byte)'X');
	
	private final byte value;
	
	private FrontendMessageType(byte value) {
		this.value = value;
	}
	
	public byte getValue() {
		return value;
	}
	
	public boolean includedInPacket() {
		return value != (byte)0xFF;
	}

}
