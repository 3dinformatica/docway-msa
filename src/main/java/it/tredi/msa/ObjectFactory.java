package it.tredi.msa;

import java.lang.reflect.Field;
import java.util.Map;

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
		Object returnObject = null;
		
		try {
			Class reflectClass = Class.forName(configuration.getClassName());
			returnObject = reflectClass.newInstance();
			
			Field[] fields = reflectClass.getDeclaredFields();
			for(Field field : fields){
				field.setAccessible(true);
				for(Map.Entry<String, String> entry : configuration.getParams().entrySet()){
					if(field.getName().equals(entry.getKey()))
						field.set(returnObject, entry.getValue());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return returnObject;
	}

}
