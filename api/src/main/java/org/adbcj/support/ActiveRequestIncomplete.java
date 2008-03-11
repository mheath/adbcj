package org.adbcj.support;

import org.adbcj.DbException;
import org.adbcj.DbSession;

public class ActiveRequestIncomplete extends DbException {
	private static final long serialVersionUID = 1L;

	public ActiveRequestIncomplete(DbSession session, String message) {
		super(session, message);
	}
}
