package org.safehaus.adbcj.postgresql.backend;

public enum AuthenticationType {
	OK,
	UNKNOWN,
	KERBEROS_5,
	CLEARTEXT_PASSWORD,
	CRYPT_PASSWORD,
	MD5_PASSWORD,
	SCM_CREDENTIAL,
	GSS,
	GSS_CONTINUE
}
