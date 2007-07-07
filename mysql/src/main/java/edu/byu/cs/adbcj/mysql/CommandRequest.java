package edu.byu.cs.adbcj.mysql;

import java.nio.charset.CharacterCodingException;

public class CommandRequest extends Request {

	private final Command command;
	private final String payload;
	
	public CommandRequest(Command command) {
		this.command = command;
		this.payload = null;
	}
	
	public CommandRequest(Command command, String payload) {
		this.command = command;
		this.payload = payload;
	}
	
	public Command getCommand() {
		return command;
	}
	
	public String getPayload() {
		return payload;
	}
	
	@Override
	int getLength(MysqlCharacterSet charset) throws CharacterCodingException {
		return 1 + ((payload == null) ? 0 : charset.encodedLength(payload));
	}
	
}
