package it.tredi.msa.configuration;

import java.util.Arrays;

import it.tredi.msa.ObjectFactory;
import it.tredi.msa.ObjectFactoryConfiguration;
import it.tredi.msa.entity.MSAConfiguration;
import it.tredi.msa.entity.MailboxConfiguration;

public class ConfigurationService {
	
	private static ConfigurationService instance;
	
	private MSAConfiguration msaConfiguration;
	
	private MailboxConfigurationReader []mailboxConfigurationReaders;
	
	private ConfigurationService() {
	}

	public static synchronized ConfigurationService getInstance() {
	    if (instance == null) {
	        instance = new ConfigurationService();
	    }
	    return instance;
	}
	
	//MsaConfigurationReader -> MsaConfiguration (config dell'archiviatore compreso tempo polling default e lista dei mailboxconfigurationreader da utilizzare)
	//Lista dei 
	
	
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
		return ret;
	}
	
}
