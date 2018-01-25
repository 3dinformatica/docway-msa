package it.tredi.msa.configuration;


public abstract class MailboxConfigurationReader {
	
	public abstract Object getRawData();
	
	public abstract Docway4MailboxConfigurationReader readConfiguration();
	

}
