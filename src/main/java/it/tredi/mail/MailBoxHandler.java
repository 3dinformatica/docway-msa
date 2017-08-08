package it.tredi.mail;

import java.io.IOException;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

public class MailBoxHandler {
	
	private Session session;
	
	public void login(Credentials credentials){
		session = SessionManager.getSessionInstance(credentials);
	}
	
	public void readMail() throws MessagingException, IOException{
		 Store store = session.getStore("pop3s");	 
		 store.connect();
		 Folder inbox = store.getFolder("INBOX");
		 inbox.open(Folder.READ_ONLY);
		 Message[] messages = inbox.getMessages();
		 
		 for (int i = 0, n = messages.length; i < n; i++) {
	         Message message = messages[i];
	         System.out.println("---------------------------------");
	         System.out.println("Email Number " + (i + 1));
	         System.out.println("Subject: " + message.getSubject());
	         System.out.println("From: " + message.getFrom()[0]);
	         System.out.println("Text: " + message.getContent().toString());
	      }
		 
		 inbox.close(false);
		 store.close();
	}
	
	public void disconnect(){
		SessionManager.destroySession();
	}
}
