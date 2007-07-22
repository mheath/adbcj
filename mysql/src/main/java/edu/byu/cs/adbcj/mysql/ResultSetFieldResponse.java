package edu.byu.cs.adbcj.mysql;


public class ResultSetFieldResponse extends Response {

	private final MysqlField field;
	
	public ResultSetFieldResponse(int packetLength, byte packetNumber, MysqlField field) {
		super(packetLength, packetNumber);
		this.field = field;
	}
	
	public MysqlField getField() {
		return field;
	}

}
