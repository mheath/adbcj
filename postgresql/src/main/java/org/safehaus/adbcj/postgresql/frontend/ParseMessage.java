package org.safehaus.adbcj.postgresql.frontend;

public class ParseMessage extends AbstractFrontendMessage {

	private final String query;
	private final String statement;
	private final int[] parameters; // TODO The param types may need to be enums, figure this out
	
	public ParseMessage(String query) {
		this(query, null, null);
	}
	
	public ParseMessage(String query, String statement) {
		this(query, statement, null);
	}
	
	public ParseMessage(String query, String statement, int[] parameters) {
		this.query = query;
		this.statement = statement;
		this.parameters = parameters;
	}

	@Override
	public FrontendMessageType getType() {
		return FrontendMessageType.PARSE;
	}
	
	public int[] getParameters() {
		return parameters;
	}
	
	public String getQuery() {
		return query;
	}
	
	public String getStatement() {
		return statement;
	}

}
