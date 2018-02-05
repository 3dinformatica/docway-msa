package it.tredi.msa.mailboxmanager;

import it.tredi.mail.MailReader;
import it.tredi.msa.entity.MailboxConfiguration;

public abstract class MailboxManager {

	private MailboxConfiguration configuration;
	private MailReader mailReader;
	
	public MailboxConfiguration getConfiguration() {
		return configuration;
	}
	
	public void setConfiguration(MailboxConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public MailReader getMailReader() {
		return mailReader;
	}
	
	public void setMailReader(MailReader mailReader) {
		this.mailReader = mailReader;
	}
	
}
