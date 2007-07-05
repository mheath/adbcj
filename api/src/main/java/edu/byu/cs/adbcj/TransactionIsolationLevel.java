package edu.byu.cs.adbcj;

public enum TransactionIsolationLevel {
	NONE,
	READ_COMMITTED,
	READ_UNCOMMITTED,
	REPEATABLE_READ,
	SERIALIZABLE
}
