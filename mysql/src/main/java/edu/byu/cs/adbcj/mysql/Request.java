package edu.byu.cs.adbcj.mysql;

import java.nio.charset.CharacterCodingException;

public abstract class Request {

	abstract int getLength(MysqlCharacterSet charset) throws CharacterCodingException;

	public byte getPacketNumber() {
		return 0;
	}
}
