package org.adbcj.perf;

/**
 *
 */
public enum Configuration {
	MYSQL_JDBC("MySQL JDBC", "jdbc:mysql://%s/adbcjtck", false, false),
	POSTGRESQL_JDBC("Postgresql JDBC", "jdbc:postgresql://%s/adbcjtck", false, false),
	MYSQL_JDBC_BATCH("MySQL JDBC Batch", "jdbc:mysql://%s/adbcjtck", false, true),
	POSTGRESQL_JDBC_BATCH("Postgresql JDBC Batch", "jdbc:postgresql://%s/adbcjtck", false, true),
	MYSQL_ADBCJ("MySQL ADBCJ MINA", "adbcj:mysql://%s/adbcjtck", true, false),
	MYSQL_ADBCJ_NETTY("MySQL ADBCJ Netty", "adbcj:mysqlnetty://%s/adbcjtck", true, false),
	POSTGRESQL_ADBCJ("Postgresql ADBCJ MINA", "adbcj:postgresql-mina://%s/adbcjtck", true, false),
	POSTGRESQL_ADBCJ_NETTY("Postgresql ADBCJ Netty", "adbcj:postgresql-netty://%s/adbcjtck", true, false),
	MYSQL_ADBCJ_NO_PIPELINING("MySQL ADBCJ No Pipelining", "adbcj:mysql://%s/adbcjtck", false, false),
	MYSQL_ADBCJ_NETTY_NO_PIPELINING("MySQL ADBCJ Netty No Pipelining", "adbcj:mysqlnetty://%s/adbcjtck", false, false),
	POSTGRESQL_ADBCJ_NO_PIPELINING("Postgresql ADBCJ MINA No Pipelining", "adbcj:postgresql-mina://%s/adbcjtck", false, false),
	POSTGRESQL_ADBCJ_NETTY_NO_PIPELINING("Postgresql ADBCJ Netty No Pipelining", "adbcj:postgresql-netty://%s/adbcjtck", false, false),
	MYSQL_ADBCJ_JDBC("MySQL ADBCJ/JDBC", "adbcj:jdbc:mysql://%s/adbcjtck", false, false),
	POSTGRESQL_ADBCJ_JDBC("Postgresql ADBCJ/JDBC", "adbcj:jdbc:postgresql://%s/adbcjtck", false, false);

	private final String s;
	private final String url;
	private final boolean pipelined;
	private final boolean batched;

	private Configuration(String s, String url, boolean pipelined, boolean batched) {
		this.s = s;
		this.url = url;
		this.pipelined = pipelined;
		this.batched = batched;
	}

	public String getUrl(String host) {
		return String.format(url, host);
	}

	public boolean isPipelined() {
		return pipelined;
	}

	public boolean isBatched() {
		return batched;
	}

	@Override
	public String toString() {
		return s;
	}
}
