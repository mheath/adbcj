package org.safehaus.adbcj.postgresql.frontend;

import org.safehaus.adbcj.postgresql.FormatCode;

public class BindMessage extends AbstractFrontendMessage {

	private final String portal;
	private final String statement;
	private final FormatCode[] parameterFormats;
	private final String[] parameterValues;
	private final FormatCode[] resultFormats;
	
	public BindMessage() {
		this(null, null, null, null, null);
	}
	
	public BindMessage(String statement) {
		this(statement, null, null, null, null);
	}
	
	public BindMessage(String statement, String portal, FormatCode[] parameterFormats, String[] parameterValues, FormatCode[] resultFormats) {
		this.statement = statement;
		this.portal = portal;
		this.parameterFormats = parameterFormats;
		this.parameterValues = parameterValues;
		this.resultFormats = resultFormats;
	}
	
	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.BIND;
	}

	public String getPortal() {
		return portal;
	}

	public String getStatement() {
		return statement;
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
