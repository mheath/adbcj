package org.adbcj.postgresql.codec;

import java.nio.charset.Charset;

/**
 * @author Mike Heath
 */
public class ConnectionState {

	private final String databaseName;
	// TODO Determine if we really need to distinguish frontend and backend charsets
	// TODO Make frontend charset configurable
	private final Charset frontendCharset = Charset.forName("UTF-8");
	// TODO Update backendCharset based on what backend returns
	private Charset backendCharset = Charset.forName("US-ASCII");

	private PgField[] fields;

	public ConnectionState(String databaseName) {
		this.databaseName = databaseName;
	}

	public Charset getBackendCharset() {
		return backendCharset;
	}

	public Charset getFrontendCharset() {
		return frontendCharset;
	}

	public void setBackendCharset(Charset backendCharset) {
		this.backendCharset = backendCharset;
	}

	public PgField[] getCurrentResultSetFields() {
		return fields;
	}

	public void setCurrentResultSetFields(PgField[] fields) {
		this.fields = fields;
	}

	public String getDatabaseName() {
		return databaseName;
	}

}
