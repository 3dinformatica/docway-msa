package it.tredi.msa.configuration;

import java.util.Arrays;
import java.util.HashSet;

import it.tredi.msa.ObjectFactory;
import it.tredi.msa.ObjectFactoryConfiguration;
import it.tredi.msa.entity.MSAConfiguration;
import it.tredi.msa.entity.MailboxConfiguration;

public class ConfigurationService {
	
	private static ConfigurationService instance;
	private MSAConfiguration msaConfiguration;
	private MailboxConfigurationReader []mailboxConfigurationReaders;
	private final static String EMAIL_DUPLICATES_NOT_ALLOWED = "Indirizzi email duplicati non concessi: '%s'. Qualora non si tratti di un errore di configurazione è possibile forzare il funzionamento tramite la property '" + MSAConfigurationReader.MAILBOXMANAGERS_ALLOW_EMAIL_DUPLICATES + "'";
	
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
		
		//if email duplicates not allowed -> check for it
		if (!getMSAConfiguration().isAllowEmailDuplicates())
			checkForduplicates(ret);
			
		return ret;
	}
	
	private void checkForduplicates(MailboxConfiguration []configurations) throws Exception {
		HashSet<String> set = new HashSet<>();
		for (int i=0; i<configurations.length; i++) {
			String key = configurations[i].getUser();
			if (set.add(key) == false) {
		    	 throw new Exception(String.format(EMAIL_DUPLICATES_NOT_ALLOWED, key));
			}
		}
	}
	
}
