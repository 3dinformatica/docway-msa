package it.tredi.msa;

import java.lang.reflect.Constructor;

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

		/*
        Class partypes[] = new Class[2];
         partypes[0] = Integer.TYPE;
         partypes[1] = Integer.TYPE;
         Constructor ct 
           = cls.getConstructor(partypes);
         Object arglist[] = new Object[2];
         arglist[0] = new Integer(37);
         arglist[1] = new Integer(47);
         Object retobj = ct.newInstance(arglist);

         
		Class<?>[] partypes = new Class[2];
		partypes[0] = String.class;
		partypes[1] = Page.class;
		Object[] arglist = new Object[2];
		arglist[0] = templateName;
		arglist[1] = this;
		Class<?> theClass = Class.forName(packageName + "." + className);
		Object theObject = theClass.getConstructor(partypes).newInstance(arglist);         */

		return object;
	}

}
