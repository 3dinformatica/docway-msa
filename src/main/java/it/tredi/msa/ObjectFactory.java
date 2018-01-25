package it.tredi.msa;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;

import ir.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.audit.AuditWriter;
import it.tredi.msa.notification.NotificationSender;

public class ObjectFactory {

	public static MailboxConfigurationReader createMailboxConfigurationReader(ObjectFactoryConfiguration mailboxConfigurationReaderConfiguration) throws Exception {
		return (MailboxConfigurationReader)createObject(mailboxConfigurationReaderConfiguration);
	}

	public static AuditWriter createAuditWriter(ObjectFactoryConfiguration auditWriterConfiguration) throws Exception {
		return (AuditWriter)createObject(auditWriterConfiguration);
	}

	public static NotificationSender createNotificationSender(ObjectFactoryConfiguration notificationSenderConfiguration) throws Exception {
		return (NotificationSender)createObject(notificationSenderConfiguration);
	}

	private static Object createObject(ObjectFactoryConfiguration configuration) throws Exception {
		Class<?> cls = Class.forName(configuration.getClassName());
		Constructor<?> ct = cls.getConstructor();
		Object object = ct.newInstance();
		
		Iterator<String> keysIterator = configuration.getParams().keySet().iterator();
		while (keysIterator.hasNext()) {
			String key = keysIterator.next();
			String value = configuration.getParams().get(key);
			String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
			Class<?>[] paramsTypes = {String.class};
			Method theMethod = cls.getMethod(methodName, paramsTypes);
			Object[] arglist = {value};
			theMethod.invoke(object, arglist);
		}

		return object;
	}

}
