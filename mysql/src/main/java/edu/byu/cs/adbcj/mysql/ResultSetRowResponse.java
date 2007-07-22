package edu.byu.cs.adbcj.mysql;

public class ResultSetRowResponse extends Response {

	private final MysqlRow row;
	
	public ResultSetRowResponse(int packetLength, byte packetNumber, MysqlRow row) {
		super(packetLength, packetNumber);
		this.row = row;
	}
	
	public MysqlRow getRow() {
		return row;
	}
	
}
