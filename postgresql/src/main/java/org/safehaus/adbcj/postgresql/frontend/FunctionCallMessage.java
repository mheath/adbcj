package org.safehaus.adbcj.postgresql.frontend;

import org.safehaus.adbcj.postgresql.FormatCode;

public class FunctionCallMessage extends AbstractFrontendMessage {

	private final int objectId;
	private final FormatCode[] parameterFormats;
	private final String[] parameterValues;
	private final FormatCode[] resultFormats;

	public FunctionCallMessage(int objectId, FormatCode[] parameterFormats, String[] parameterValues, FormatCode[] resultFormats) {
		this.objectId = objectId;
		this.parameterFormats = parameterFormats;
		this.parameterValues = parameterValues;
		this.resultFormats = resultFormats;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.FUNCTION_CALL;
	}

	public int getObjectId() {
		return objectId;
	}

	public FormatCode[] getParameterFormats() {
		return parameterFormats;
	}

	public String[] getParameterValues() {
		return parameterValues;
	}

	public FormatCode[] getResultFormats() {
		return resultFormats;
	}

}
