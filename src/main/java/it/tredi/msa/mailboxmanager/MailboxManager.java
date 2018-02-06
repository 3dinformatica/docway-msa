package it.tredi.msa.mailboxmanager;

import it.tredi.mail.MailReader;
import it.tredi.msa.entity.MailboxConfiguration;

public abstract class MailboxManager implements Runnable {

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
	
	//TODO - utilizzare template pattern
	//servono dei metodi per la gestione della sessione di archiviazione (ad es per stabilire la connessione al db)
	
	public void run() {
		// TODO Auto-generated method stub
		
		//startSession
		
		//getNextMail (while)
			//handle mail
			//gestione cancellazione / spostamento
			//mailDone
		
		//endSession
		
	}
	
}
