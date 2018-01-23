package ir.tredi.msa.configuration;

import it.tredi.msa.entity.MailboxConfiguration;

public abstract class MailboxConfigurationReader {
	
	public abstract Object getRawData();
	
	public abstract MailboxConfiguration readConfiguration();

}
