package it.tredi.msa;

import ir.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.notification.NotificationSender;

public class ObjectFactory {
	
	public static NotificationSender createNotificationSender(ObjectFactoryConfiguration notificationSenderConfiguration) {
		return (NotificationSender)createObject(notificationSenderConfiguration);
	}

	public static MailboxConfigurationReader createMailboxConfigurationReader(ObjectFactoryConfiguration mailboxConfigurationReaderConfiguration) {
		return (MailboxConfigurationReader)createObject(mailboxConfigurationReaderConfiguration);
	}
	
	private static Object createObject(ObjectFactoryConfiguration configuration) {
		
		//TODO - utilizzare la reflection
		return null;
	}

}
