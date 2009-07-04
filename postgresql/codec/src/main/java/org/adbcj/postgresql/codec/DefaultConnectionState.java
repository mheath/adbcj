package org.adbcj.postgresql.codec;

import java.nio.charset.Charset;

/**
 * @author Mike Heath
 */
public class DefaultConnectionState implements ConnectionState {

	private final String databaseName;
	private Charset backendCharset;
	private PgField[] fields;

	public DefaultConnectionState(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public Charset getBackendCharset() {
		return backendCharset;
	}

	public void setBackendCharset(Charset backendCharset) {
		this.backendCharset = backendCharset;
	}

	@Override
	public PgField[] getCurrentResultSetFields() {
		return fields;
	}

	@Override
	public void setCurrentResultSetFields(PgField[] fields) {
		this.fields = fields;
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

}
