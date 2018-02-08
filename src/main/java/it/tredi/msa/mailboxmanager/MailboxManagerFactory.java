package it.tredi.msa.mailboxmanager;

import java.lang.reflect.Constructor;
import it.tredi.mail.MailClientHelper;
import it.tredi.msa.entity.MailboxConfiguration;

public class MailboxManagerFactory {
	
	public static MailboxManager createMailboxManager(MailboxConfiguration mailboxConfiguration) throws Exception {
		Class<?> cls = Class.forName(mailboxConfiguration.getMailboxManagerClassName());
		Constructor<?> ct = cls.getConstructor();
		MailboxManager mailboxManager = (MailboxManager)ct.newInstance();
		mailboxManager.setConfiguration(mailboxConfiguration);
		mailboxManager.setMailReader(MailClientHelper.createMailReader(mailboxConfiguration.getHost(), mailboxConfiguration.getPort(), mailboxConfiguration.getUser(), mailboxConfiguration.getPassword(), mailboxConfiguration.getProtocol()));
		return (MailboxManager)mailboxManager;
	}
	
	public static MailboxManager []createMailboxManagers(MailboxConfiguration[] mailboxConfigurations) throws Exception {
		MailboxManager []mailboxManagers = new MailboxManager[mailboxConfigurations.length];
		int i = 0;
		for (MailboxConfiguration mailboxConfiguration:mailboxConfigurations)
			mailboxManagers[i++] = createMailboxManager(mailboxConfiguration);
		return mailboxManagers;
	}

}