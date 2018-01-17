package ir.tredi.msa.configuration;

import it.tredi.msa.entity.MSAConfiguration;

public class ConfigurationService {
	
	private static ConfigurationService instance;
	
	private MSAConfiguration msaConfiguration;
	
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
	
	
	public MSAConfiguration getMSAConfiguration() {
		if (msaConfiguration == null)
			msaConfiguration = new MSAConfigurationReader().read();
		return msaConfiguration;
	}

}



