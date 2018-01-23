package it.tredi.msa;

import ir.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.audit.AuditWriter;
import it.tredi.msa.notification.NotificationSender;

public class ObjectFactory {
	
	public static MailboxConfigurationReader createMailboxConfigurationReader(ObjectFactoryConfiguration mailboxConfigurationReaderConfiguration) {
		return (MailboxConfigurationReader)createObject(mailboxConfigurationReaderConfiguration);
	}	

	public static AuditWriter createAuditWriter(ObjectFactoryConfiguration auditWriterConfiguration) {
		return (AuditWriter)createObject(auditWriterConfiguration);
	}	
	
	public static NotificationSender createNotificationSender(ObjectFactoryConfiguration notificationSenderConfiguration) {
		return (NotificationSender)createObject(notificationSenderConfiguration);
	}
	
	private static Object createObject(ObjectFactoryConfiguration configuration) {
		
		//TODO - utilizzare la reflection
		return null;
	}

}
