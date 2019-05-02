package it.tredi.msa.configuration;

import it.tredi.msa.ObjectFactoryConfiguration;

public class MSAConfiguration {
	
	private ObjectFactoryConfiguration []mailboxConfigurationReadersConfiguration;
	private ObjectFactoryConfiguration auditWriterConfiguration;
	private ObjectFactoryConfiguration notificationSenderConfiguration;
	private int mailboxManagersDelay;
	
	/**
	 * Intervallo di tempo (ms.) tra 2 salvataggi di messaggi email durante l'orario di lavoro
	 */
	private int mailboxManagersWorkTimeMailDelay;
	
	private int mailboxManagersPoolsize;
	private int mailboxManagersRefreshTime;
	private boolean mailboxManagersHotReloading = false;
	private boolean allowEmailDuplicates;
	Object rawData;
	
	private int mailboxManagersParseThreadPoolsize;
	private int mailboxManagersParseThreadActivationThreshold;
	
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

	public int getMailboxManagersWorkTimeMailDelay() {
		return mailboxManagersWorkTimeMailDelay;
	}

	public void setMailboxManagersWorkTimeMailDelay(int mailboxManagersWorkTimeMailDelay) {
		this.mailboxManagersWorkTimeMailDelay = mailboxManagersWorkTimeMailDelay;
	}
	
	public int getMailboxManagersPoolsize() {
		return mailboxManagersPoolsize;
	}

	public void setMailboxManagersPoolsize(int mailboxManagersPoolsize) {
		this.mailboxManagersPoolsize = mailboxManagersPoolsize;
	}
	
	public int getMailboxManagersRefreshTime() {
		return mailboxManagersRefreshTime;
	}

	public void setMailboxManagersRefreshTime(int mailboxManagersPoolsize) {
		this.mailboxManagersRefreshTime = mailboxManagersPoolsize;
	}

	public boolean isMailboxManagersHotReloading() {
		return mailboxManagersHotReloading;
	}

	public void setMailboxManagersHotReloading(boolean mailboxManagersHotReloading) {
		this.mailboxManagersHotReloading = mailboxManagersHotReloading;
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
	
	public int getMailboxManagersParseThreadPoolsize() {
		return mailboxManagersParseThreadPoolsize;
	}

	public void setMailboxManagersParseThreadPoolsize(int mailboxManagersParseThreadPoolsize) {
		this.mailboxManagersParseThreadPoolsize = mailboxManagersParseThreadPoolsize;
	}

	public int getMailboxManagersParseThreadActivationThreshold() {
		return mailboxManagersParseThreadActivationThreshold;
	}

	public void setMailboxManagersParseThreadActivationThreshold(int mailboxManagersParseThreadActivationThreshold) {
		this.mailboxManagersParseThreadActivationThreshold = mailboxManagersParseThreadActivationThreshold;
	}
	
}
