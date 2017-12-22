package it.tredi.mail;

import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;

public class MailReader extends MailClient {
	
	//TODO - JAVADOC + CLIENT USAGE SAMPLES
	
	private boolean imap;
	private Folder folder;
	private Store store;
	
	public final static String INBOX = "INBOX";
	public final static String NO_DEFAULT_FOLDER_IN_STORE_MESSAGE = "Default folder not found in store";
	public static final String POP3_UNSUPPORTED_FOLDER_MESSAGE = "Invalid folder name. POP3 supports only INBOX folder";
	public static final String UNEXISTING_FOLDER_MESSAGE = "Invalid folder name";
	public static final String POP3_UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported using POP3";
	public static final String UNSUPPORTED_OPERATION_DELETE_NOT_EMPTY_FOLDER_MESSAGE = "Non-empty folder deletion not supported";
	public static final String UNSUPPORTED_OPERATION_OPENED_FOLDER_DELETION_MESSAGE = "Opened folder deletion not supported";
	
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
     	
     	folder = store.getDefaultFolder();
     	if (!folder.exists())
     		throw new MessagingException(NO_DEFAULT_FOLDER_IN_STORE_MESSAGE);
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
		closeFolder();
		store.close();
		store = null;
	}

	public Store getStore() {
		return store;
	}

	public Folder getFolder() {
		return folder;
	}
	
	public void openInboxFolder() throws MessagingException {
		openFolder(INBOX);
	}
	
	public void openFolder(String folderName) throws MessagingException {
		closeFolder(); //close current folder before open another one
		if (!isImap() && !folderName.equalsIgnoreCase(INBOX)) //POP3
			throw new MessagingException(POP3_UNSUPPORTED_FOLDER_MESSAGE);
		folder = store.getFolder(folderName); //1st try - store child
		if (!folder.exists())
			throw new MessagingException(UNEXISTING_FOLDER_MESSAGE);
		folder.open(Folder.READ_WRITE);
	}
	
	public void closeFolder() throws MessagingException {
		if (folder.exists() && folder.isOpen()) {
			if (isImap())
				folder.expunge();
			folder.close(true); //close with expunge the flag set to true
			folder = store.getDefaultFolder();
		}		
	}
	
	public boolean createFolder(String folderName) throws MessagingException {
		if (!isImap()) //POP3
			throw new MessagingException(POP3_UNSUPPORTED_OPERATION_MESSAGE);
		Folder _folder = store.getFolder(folderName);
		if (!_folder.exists())
			return _folder.create(Folder.HOLDS_MESSAGES);
		return false;
	}
	
	public boolean deleteFolder(String folderName) throws MessagingException {
		if (!isImap()) //POP3
			throw new MessagingException(POP3_UNSUPPORTED_OPERATION_MESSAGE);
		if (folder.getFullName().equalsIgnoreCase(folderName))
			throw new MessagingException(UNSUPPORTED_OPERATION_OPENED_FOLDER_DELETION_MESSAGE);		
		Folder _folder = store.getFolder(folderName);
		if (_folder.exists()) {
			_folder.open(Folder.READ_WRITE);
			if (_folder.getMessageCount() > 0) {
				_folder.close();
				throw new MessagingException(UNSUPPORTED_OPERATION_DELETE_NOT_EMPTY_FOLDER_MESSAGE);
			}
			_folder.close();
			return _folder.delete(false);
		}
		return false;
	}
	
	public Message []getMessages() throws MessagingException {
		/*
		if (!openFolderCalled)
			openInboxFolder();
		MessageUtils []messages = new MessageUtils[folder.getMessageCount()];
		for (int i=1; i<=folder.getMessageCount(); i++)
				messages[i-1] = new MessageUtils(folder.getMessage(i));
		return messages;
		*/
		return folder.getMessages();
		
//TODO - implement
//TODO - probabilmente deve diventare getHeaders e avere a che fare con l'ultimo metodi di ricerca -> farne uno solo
	}
	
	public MimeMessage loadFullMessage(Message message) throws MessagingException {
		return new MimeMessage((MimeMessage)message);
		
//TODO - TEST THIS ONE
	}
	
	public void deleteMessage(Message message) throws MessagingException {
		message.setFlag(Flags.Flag.DELETED, true);
		/*
		 * Gmail does not follow the normal IMAP conventions for deleting messages. Marking a message as deleted and then expunging the folder simply removes the 
		 * current folder's "label" from the message. 
		 * The message will still appear in the "[Gmail]/All Mail" folder. To delete a message, copy the message to the "[Gmail]/Trash" folder, 
		 * which will immediately remove the message from the current folder. To permanently remove a message, open the "[Gmail]/Trash" folder, 
		 * mark the message deleted (msg.setFlag(Flags.Flag.DELETED, true);), and expunge the folder (folder.close(true);). 
		 */
	}
	
	public void copyMessageToFolder(Message message, String destinationFolderName) throws MessagingException {
		if (!isImap()) //POP3
			throw new MessagingException(POP3_UNSUPPORTED_OPERATION_MESSAGE);		
		Folder _folder = store.getFolder(destinationFolderName);
		if (!_folder.exists())
			throw new MessagingException(UNEXISTING_FOLDER_MESSAGE);
		Message []_messages = {message};
		_folder.appendMessages(_messages);
	}
	
	public void queryMessagesInFolder() {
		//TODO - implement
	}

}
