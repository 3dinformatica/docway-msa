package it.tredi.mail;

import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;

/*
 * 
 * An idea about the new project. 
 * Here I am trying to keep the mail Session for a mailbox open and create another only
 * when we try to change the mailbox account. 
 * I created Credentials, a Credentials object holds all the necessary fields required for login.
 * SessionManager is an object the can return a Session instance.  The getSessionInstance(Credentials) gets
 * a Credentials object as a parameter and returns a new Session Instance only if the Credentials are different
 * from the current one. 
 * MailBoxHandle uses a session to do what is needed with the mailbox. 
 * 
 *  This is just an idea. Criticism is welcomed. 
 * 
 */

public class Main {

	public static void main(String[] args) throws MessagingException, IOException {
		// TODO Auto-generated method stub
		String host = "pop3s.aruba.it";
		String username = "agjoni@3dial.eu";
		String password = "3dinformatica";
		String port = "995";
		
		Properties props = new Properties();
		props.put("mail.pop3s.host", host);
		props.put("mail.pop3s.port", port);
		props.put("mail.pop3s.starttls.enable", "true");
		
		Credentials cre = new Credentials();
		cre.setProps(props);
		cre.setUsername(username);
		cre.setPassword(password);
		cre.setPop3sPort(port);
		cre.setPop3sHost(host);
		
		MailBoxHandler mailbox = new MailBoxHandler();
		
		mailbox.login(cre);
		mailbox.readMail();
		
		cre.setUsername("lpapa@3dial.eu");
		cre.setPassword("Environmental777");
		
		mailbox.login(cre);
		
		mailbox.readMail();
		mailbox.disconnect();
	}

}
