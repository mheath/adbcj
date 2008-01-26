package org.adbcj.postgresql;

public enum ErrorField {
	SEVERITY('S', "Severity"),
	CODE('C', "Code"),
	MESSAGE('M', "Message"),
	DETAIL('D', "Detail"),
	HINT('H', "Hint"),
	POSITION('P', "Position"),
	INTERNAL_POSITION('p', "Internal Position"),
	INTERNAL_QUERY('q', "Internal query"),
	WHERE('W', "Where"),
	FILE('F', "File"),
	LINE('L', "Line"),
	ROUTINE('R', "Routine");
	
	private final byte token;
	private final String fieldName;
	
	private ErrorField(char token, String fieldName) {
		this.token = (byte)token;
		this.fieldName = fieldName;
	}
	
	public byte getToken() {
		return token;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public static ErrorField toErrorField(byte token) {
		for (ErrorField field : values()) {
			if (token == field.token) {
				return field;
			}
		}
		return null;
	}
}
