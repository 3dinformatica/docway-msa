package it.tredi.msa.test.misc;

import org.junit.Test;
import org.junit.runners.MethodSorters;
import it.tredi.msa.Services;


import org.junit.FixMethodOrder;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NotificationServiceTests {
	
	@Test
	public void test_001_getMSAConfiguration() throws Exception {
		Services.getConfigurationService().getMSAConfiguration();
	}

	@Test
	public void test_002_createNotificationService() throws Exception {
		Services.getNotificationService().init();
	}
	
	@Test
	public void test_003_createNotificationService() throws Exception {
		Services.getNotificationService().notifyError("Messaggio di test! Errore in fase di archiviazione!");
	}

	
}
