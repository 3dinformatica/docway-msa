package it.tredi.mail;

import javax.mail.PasswordAuthentication;

public class Authenticator extends javax.mail.Authenticator {
	
	private String username;
	private String password;
	
	public Authenticator(String username, String password) {
		super();
		this.username = username;
		this.password = password;
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(username, password);
	}
}
