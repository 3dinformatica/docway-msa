package it.tredi.msa;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;

import it.tredi.msa.audit.AuditWriter;
import it.tredi.msa.configuration.MailboxConfigurationReader;
import it.tredi.msa.notification.NotificationSender;

/**
 * Factory class used to create three type of objects: MailboxCondifuration, AuditWriter, NotificationSender
 * New objects are created via reflection from a ObjectFactoryConfiguration object. ObjectFactoryConfiguration objects are POJOs containing class name and a Map of properties. 
 * Since new objects are created via default constructor and all the properties are injected via setter methods, every property must have a corresponding setter(String) method in the new object class.
 *   
 * @author sstagni
 *
 */
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
