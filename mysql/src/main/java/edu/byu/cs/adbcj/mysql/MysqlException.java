package edu.byu.cs.adbcj.mysql;

import edu.byu.cs.adbcj.DbException;

public class MysqlException extends DbException {
	private static final long serialVersionUID = 1L;

	public MysqlException(String message, Throwable cause) {
		super(message, cause);
	}

	public MysqlException(String message) {
		super(message);
	}

}
