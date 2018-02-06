package it.tredi.msa.mailboxmanager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;

import it.tredi.msa.entity.MailboxConfiguration;

public class MailboxManagerFactory {
	
	public MailboxManager createMailboxManager(MailboxConfiguration mailboxConfiguration) throws Exception {
		Class<?> cls = Class.forName(mailboxConfiguration.getMailboxManagerClassName());
		Constructor<?> ct = cls.getConstructor();
		MailboxManager mailboxManager = (MailboxManager)ct.newInstance();
		mailboxManager.setConfiguration(mailboxConfiguration);
		
		//TODO - creare il mail reader e 
		//mailReader
		//mailboxManager.setMailReader(mailReader);
		
		return (MailboxManager)mailboxManager;
	}

}
