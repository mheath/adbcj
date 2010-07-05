package org.adbcj.tck;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;

import java.util.Random;

/**
 *
 */
public class PopulateLarge {

	public static void main(String[] args) throws Exception {
		ConnectionManager mysqlCM = ConnectionManagerProvider.createConnectionManager("adbcj:mysqlnetty://localhost/adbcjtck", "adbcjtck", "adbcjtck");
		ConnectionManager pgCM = ConnectionManagerProvider.createConnectionManager("adbcj:postgresql-netty://localhost/adbcjtck", "adbcjtck", "adbcjtck");

		Connection mysql = mysqlCM.connect().getUninterruptably();
		Connection pg = pgCM.connect().getUninterruptably();

		final String insertTemplate = "INSERT INTO large (a, b, c) VALUES ('%s', '%s', '%s')"; 
		for (int i = 0; i < 998; i++) {
			String a = randString();
			String b = randString();
			String c = randString();
			final String insert = String.format(insertTemplate, a, b, c);
			mysql.executeUpdate(insert).get();
			pg.executeUpdate(insert).get();
		}
//		mysql.close(false).get();
//		pg.close(false).get();
//		mysqlCM.close(true);
//		pgCM.close(true);
	}

	private static String randString() {
		Random rand = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 255; i++) {
			char c = (char)(rand.nextInt(26) + 65);
			sb.append(c);
		}
		return sb.toString();
	}

}
