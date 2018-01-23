package it.tredi.msa.entity;

import java.util.Properties;

import it.tredi.msa.ObjectFactoryConfiguration;

public class MSAConfiguration {
	
	Properties rawData;
	
	ObjectFactoryConfiguration []mailboxConfigurationReadersConfiguration;
	ObjectFactoryConfiguration auditWriterConfiguration;
	ObjectFactoryConfiguration notificationSenderConfiguration;
	
	public Properties getRawData() {
		return rawData;
	}
	
	public void setRawData(Properties rawData) {
		this.rawData = rawData;
	}
	
	public ObjectFactoryConfiguration[] getMailboxConfigurationReadersConfiguration() {
		return mailboxConfigurationReadersConfiguration;
	}
	
	public void setMailboxConfigurationReadersConfiguration(
			ObjectFactoryConfiguration[] mailboxConfigurationReadersConfiguration) {
		this.mailboxConfigurationReadersConfiguration = mailboxConfigurationReadersConfiguration;
	}
	
	public ObjectFactoryConfiguration getAuditWriterConfiguration() {
		return auditWriterConfiguration;
	}
	
	public void setAuditWriterConfiguration(ObjectFactoryConfiguration auditWriterConfiguration) {
		this.auditWriterConfiguration = auditWriterConfiguration;
	}
	
	public ObjectFactoryConfiguration getNotificationSenderConfiguration() {
		return notificationSenderConfiguration;
	}
	
	public void setNotificationSenderConfiguration(ObjectFactoryConfiguration notificationSenderConfiguration) {
		this.notificationSenderConfiguration = notificationSenderConfiguration;
	}
	
	//private MailboxConfigurationReaderConfiguration []mailboxConfigurationReaderConfigurations;


}
