package it.tredi.msa.entity;

import it.tredi.msa.ObjectFactoryConfiguration;

public class MSAConfiguration {
	
	private ObjectFactoryConfiguration []mailboxConfigurationReadersConfiguration;
	private ObjectFactoryConfiguration auditWriterConfiguration;
	private ObjectFactoryConfiguration notificationSenderConfiguration;
	private int mailboxManagersDelay;
	private int mailboxManagersPoolsize;
	private boolean allowEmailDuplicates;
	Object rawData;
	
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

	public int getMailboxManagersDelay() {
		return mailboxManagersDelay;
	}

	public void setMailboxManagersDelay(int mailboxManagersDelay) {
		this.mailboxManagersDelay = mailboxManagersDelay;
	}

	public int getMailboxManagersPoolsize() {
		return mailboxManagersPoolsize;
	}

	public void setMailboxManagersPoolsize(int mailboxManagersPoolsize) {
		this.mailboxManagersPoolsize = mailboxManagersPoolsize;
	}

	public boolean isAllowEmailDuplicates() {
		return allowEmailDuplicates;
	}

	public void setAllowEmailDuplicates(boolean allowEmailDuplicates) {
		this.allowEmailDuplicates = allowEmailDuplicates;
	}

	public Object getRawData() {
		return rawData;
	}

	public void setRawData(Object rawData) {
		this.rawData = rawData;
	}
	
}
