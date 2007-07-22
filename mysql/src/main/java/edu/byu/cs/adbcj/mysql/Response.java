package edu.byu.cs.adbcj.mysql;

public abstract class Response {

	private final int packetLength;
	private final byte packetNumber;
	
	public Response(int packetLength, byte packetNumber) {
		this.packetLength = packetLength;
		this.packetNumber = packetNumber;
	}
	
	public int getPacketLength() {
		return packetLength;
	}
	
	public byte getPacketNumber() {
		return packetNumber;
	}

}
