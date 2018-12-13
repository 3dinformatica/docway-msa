package it.tredi.msa.test.misc;

import org.junit.Test;
import org.junit.runners.MethodSorters;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MailboxManagerFactory;

import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MailboxManagerTests {
	
	private static MailboxManager []mailboxManagers;
	
	/*
	@Test
	public void test_001_createConfigurationService() throws Exception {
		Services.getConfigurationService().init();
	}
	
	@Test
	public void test_002_createMailboxManagers() throws Exception {
		MailboxConfiguration []configurations = Services.getConfigurationService().readMailboxConfigurations();
		mailboxManagers = MailboxManagerFactory.createMailboxManagers(configurations);
	}

	@Test
	public void test_003_runMailboxManagers() throws Exception {
		mailboxManagers[0].run();
	}
	*/
	
}
