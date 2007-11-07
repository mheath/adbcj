package org.safehaus.adbcj.postgresql;

import org.safehaus.adbcj.support.DefaultResultSet;

public class PgResultSet extends DefaultResultSet {

	public PgResultSet(PgConnection connection, int fieldCount) {
		super(connection, fieldCount);
	}
	
	@Override
	public PgConnection getSession() {
		return (PgConnection)super.getSession();
	}

}
