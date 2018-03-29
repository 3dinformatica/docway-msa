package it.tredi.msa.configuration;

import java.util.Arrays;
import java.util.HashSet;

import it.tredi.msa.ObjectFactory;
import it.tredi.msa.ObjectFactoryConfiguration;

public class ConfigurationService {
	
	private static ConfigurationService instance;
	private MSAConfiguration msaConfiguration;
	private MailboxConfigurationReader []mailboxConfigurationReaders;
	private final static String EMAIL_DUPLICATES_NOT_ALLOWED = "Indirizzi email duplicati non concessi: '%s'. Qualora non si tratti di un errore di configurazione è possibile forzare il funzionamento tramite la property '" + MSAConfigurationReader.MAILBOXMANAGERS_ALLOW_EMAIL_DUPLICATES_PROPERTY + "'";
	private final static String MAILBOX_NAME_DUPLICATES_NOT_ALLOWED = "Nome di casella di posta duplicato: '%s'";
	
	private ConfigurationService() {
	}

	public static synchronized ConfigurationService getInstance() {
	    if (instance == null) {
	        instance = new ConfigurationService();
	    }
	    return instance;
	}
	
	public MSAConfiguration getMSAConfiguration() throws Exception {
		if (msaConfiguration == null)
			msaConfiguration = new MSAConfigurationReader().read();
		return msaConfiguration;
	}
	
	public void init() throws Exception {
		ObjectFactoryConfiguration []configurations = getMSAConfiguration().getMailboxConfigurationReadersConfiguration();
		mailboxConfigurationReaders = new MailboxConfigurationReader[configurations.length];
		for (int i=0; i<configurations.length; i++) {
			mailboxConfigurationReaders[i] = ObjectFactory.createMailboxConfigurationReader(configurations[i]);
		}
	}	
	
	public MailboxConfiguration []readMailboxConfigurations() throws Exception {
		MailboxConfiguration []ret = {};
		for (MailboxConfigurationReader mailboxConfigurationReader:mailboxConfigurationReaders) {
			MailboxConfiguration []confs = mailboxConfigurationReader.readMailboxConfigurations();
			int offset = ret.length;
			ret = Arrays.copyOf(ret, ret.length + confs.length);	
			System.arraycopy(confs, 0, ret, offset, confs.length);
		}
		
		//check for name duplicates
		checkForduplicates(ret, true);
		
		//if email duplicates not allowed -> check for it
		if (!getMSAConfiguration().isAllowEmailDuplicates())
			checkForduplicates(ret, false);
			
		return ret;
	}
	
	private void checkForduplicates(MailboxConfiguration []configurations, boolean checkForName) throws Exception {
		HashSet<String> set = new HashSet<>();
		for (int i=0; i<configurations.length; i++) {
			String key = checkForName? configurations[i].getName() : configurations[i].getUser();
			if (set.add(key) == false) {
		    	 throw new Exception(checkForName? String.format(MAILBOX_NAME_DUPLICATES_NOT_ALLOWED, key) : String.format(EMAIL_DUPLICATES_NOT_ALLOWED, key));
			}
		}
	}
	
}
