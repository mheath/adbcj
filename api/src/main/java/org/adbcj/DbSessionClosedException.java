package org.adbcj;

public class DbSessionClosedException extends DbException {

	private static final long serialVersionUID = 1L;

	public DbSessionClosedException(DbSession session, String message, Throwable cause) {
		super(session, message, cause);
	}

	public DbSessionClosedException(DbSession session, String message) {
		super(session, message);
	}

	public DbSessionClosedException(DbSession session, Throwable cause) {
		super(session, cause);
	}

	public DbSessionClosedException(DbSession session) {
		super(session);
	}

}
