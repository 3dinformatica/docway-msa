package it.tredi.msa.test.manager;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;

import com.sun.mail.imap.SortTerm;

import it.tredi.mail.MailAccount;
import it.tredi.mail.MailClient;
import it.tredi.mail.MailReader;

public class DummyMailReader extends MailReader {

	@Override
	public MailClient init(MailAccount account) {
		return this;
	}
	
	@Override
	public boolean isImap() {
		return true;
	}

	@Override
	public void connect() throws MessagingException {
	}
	
	@Override
	public void disconnect() throws MessagingException {
	}
	
	@Override
	public void openInboxFolder() throws MessagingException {
	}
	
	@Override
	public void openFolder(String folderName) throws MessagingException {
	}
	
	@Override
	public void closeFolder() throws MessagingException {
	}

	@Override
	public boolean createFolder(String folderName) throws MessagingException {
		return true;
	}

	@Override
	public boolean deleteFolder(String folderName) throws MessagingException {
		return true;
	}
	
	@Override
	public Message[] getMessages(SortTerm sortTerm, SearchTerm searchTerm) throws MessagingException {
		return null;
	}
	
	@Override
	public void deleteMessage(Message message) throws MessagingException {
	}

	@Override
	public void copyMessageToFolder(Message message, String destinationFolderName) throws MessagingException {
	}
	
}
