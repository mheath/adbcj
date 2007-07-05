package edu.byu.cs.adbcj.mysql;

public class LoginCredentials {

	private final String userName;
	private final String password;
	private final String database;
	
	public LoginCredentials(String userName, String password, String database) {
		this.userName = userName;
		this.password = password;
		this.database = database;
	}
	
	public String getDatabase() {
		return database;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getUserName() {
		return userName;
	}
	
}
