package it.tredi.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

public class SessionManager {
	private static Session session;
    private static  Credentials credentials;
	
	private SessionManager(){
		
	}
	
	public static Session getSessionInstance(Credentials cred){
		
		final String username = cred.getUsername();
		final String password = cred.getPassword();
		if(session == null && credentials == null){
			credentials = cred;
			session = Session.getDefaultInstance(credentials.getProps(), new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
		               return new PasswordAuthentication(username, password);
		            }
		         });
			System.out.println("Null credentials, creating new Session");
			
		} 
		else if (!credentials.equals(cred)){
			credentials = cred;
			session = Session.getDefaultInstance(credentials.getProps(), new javax.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
		               return new PasswordAuthentication(username, password);
		            }
		         });
			System.out.println("Different credentials, creating new Session");
		}
		
		return session;
	}
	
	public static void destroySession(){
		session = null;
		credentials = null;
	}
}
