package edu.byu.cs.adbcj.mysql;

public class ErrorResponse extends Response {

	private final int errorNumber;
	private final String sqlState;
	private final String message;
	
	public ErrorResponse(int length, byte packetNumber, int errorNumber, String sqlState, String message) {
		super(length, packetNumber);
		this.errorNumber = errorNumber;
		this.sqlState = sqlState;
		this.message = message;
	}

	public int getErrorNumber() {
		return errorNumber;
	}

	public String getSqlState() {
		return sqlState;
	}

	public String getMessage() {
		return message;
	}

}
