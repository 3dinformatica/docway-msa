package it.tredi.msa;

import ir.tredi.msa.configuration.ConfigurationService;

public class Services {
	
	public static ConfigurationService getConfigurationService() {
		return ConfigurationService.getInstance();
	}

}
