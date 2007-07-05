package edu.byu.cs.adbcj.mysql;

public abstract class Request {

	abstract int getLength();

	public byte getPacketNumber() {
		return 0;
	}
}
