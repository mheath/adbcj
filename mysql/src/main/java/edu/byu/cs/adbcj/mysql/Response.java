package edu.byu.cs.adbcj.mysql;

public abstract class Response {

	private final int length;
	private final byte packetNumber;
	
	public Response(int length, byte packetNumber) {
		this.length = length;
		this.packetNumber = packetNumber;
	}
	
	public int getLength() {
		return length;
	}
	
	public byte getPacketNumber() {
		return packetNumber;
	}

}
