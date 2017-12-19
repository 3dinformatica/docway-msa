package it.tredi.mail;

import java.util.Properties;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

public class MailReader extends MailClient {
	
	//TODO - implement all methods
	//TODO - add missing methods	
	
	private boolean imap;
	private Folder folder;
	private Store store;
	
	public MailReader() {
	}
	
	@Override
	public MailClient init(MailAccount account) {
		super.init(account);
		imap = false;
		if (account.getProtocol() == Protocol.IMAP || account.getProtocol() == Protocol.IMAPS)
			imap = true;
		return this;
	}
	
	public boolean isImap() {
		return imap;
	}

	@Override
	public void connect() throws MessagingException {
		super.connect();
     	store = session.getStore(sessionProperties.getProperty("mail.store.protocol"));
     	if (account.getPort() != -1)
     		store.connect(account.getHost(), account.getPort(), account.getUserName(), account.getPassword());
     	else
     		store.connect(account.getHost(), account.getUserName(), account.getPassword());
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	protected Properties buildSessionProperties() {
		Properties props = new Properties(System.getProperties());
		
		switch (account.getProtocol()) {
			case POP3:
				props.put("mail.store.protocol", "pop3");
				break;
			case POP3S:
				props.put("mail.store.protocol", "pop3s");
				props.put("mail.pop3.starttls.enable","true");
				break;
			case IMAP:
				props.put("mail.store.protocol", "imap");			
				break;
			case IMAPS:
				props.put("mail.store.protocol", "imaps");
				props.put("mail.imap.starttls.enable","true");
				break;				
		}
		
		//IMAP
		if (imap) {
			props.put("mail.imap.host", account.getHost());
			if (account.getPort() != -1)
				props.put("mail.imap.port", Integer.toString(account.getPort()));
			props.put("mail.imap.connectiontimeout", Long.toString(connectionTimeout));
			props.put("mail.imap.timeout", Long.toString(socketTimeout));			
		}
		//POP3
		else {
			props.put("mail.pop3.host", account.getHost());
			if (account.getPort() != -1)
				props.put("mail.pop3.port", Integer.toString(account.getPort()));
			props.put("mail.pop3.connectiontimeout", Long.toString(connectionTimeout));
			props.put("mail.pop3.timeout", Long.toString(socketTimeout));
		}

		return props;
	}	

	@Override
	public void disconnect() throws MessagingException {
		if (folder != null && folder.isOpen()) {
			folder.expunge();
			folder.close();
		}
		store = null;
	}

	public Store getStore() {
		return store;
	}

	public void openFolder() {
		
	}
	
	public void createFolder() {
		
	}
	
	public boolean existsFolder() {
		return false;
	}
	
	public void getMessages() {
		
	}
	
	public void deleteMessage() {
		
	}
	
	public void copyMessageToFolder() {
		
	}
	
	public void queryMessagesInFolder() {
		
	}

}
