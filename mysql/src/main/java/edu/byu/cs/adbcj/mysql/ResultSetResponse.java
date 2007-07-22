package edu.byu.cs.adbcj.mysql;

public class ResultSetResponse extends Response {

	private final int fieldCount;
	private final Long extra;
	
	public ResultSetResponse(int length, byte packetNumber, int fieldCount, Long extra) {
		super(length, packetNumber);
		this.fieldCount = fieldCount;
		this.extra = extra;
	}
	
	public int getFieldCount() {
		return fieldCount;
	}
	
	public Long getExtra() {
		return extra;
	}

}
