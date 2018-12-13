package it.tredi.msa.test.misc;

import org.junit.Test;
import org.junit.runners.MethodSorters;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.MailboxConfiguration;

import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationServiceTests {
	
	//@Test
	public void test_001_createConfigurationService() throws Exception {
		Services.getConfigurationService().init();
	}
	
	//@Test
	public void test_002_readMailboxConfigurations() throws Exception {
		MailboxConfiguration []configurations = Services.getConfigurationService().readMailboxConfigurations();
	}
	
}
