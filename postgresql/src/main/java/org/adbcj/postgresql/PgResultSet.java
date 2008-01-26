package org.adbcj.postgresql;

import org.adbcj.support.DefaultResultSet;

public class PgResultSet extends DefaultResultSet {

	public PgResultSet(PgConnection connection) {
		super(connection);
	}
	
	@Override
	public PgConnection getSession() {
		return (PgConnection)super.getSession();
	}

}
