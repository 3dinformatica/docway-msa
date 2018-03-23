package it.tredi.msa;

import it.tredi.msa.audit.AuditService;
import it.tredi.msa.configuration.ConfigurationService;
import it.tredi.msa.notification.NotificationService;

public class Services {
		
	public static ConfigurationService getConfigurationService() {
		return ConfigurationService.getInstance();
	}

	public static AuditService getAuditService() {
		return AuditService.getInstance();
	}
	
	public static NotificationService getNotificationService() {
		return NotificationService.getInstance();
	}
	
	public static void init() throws Exception {
		
		//load MSAConfiguration
		Services.getConfigurationService().getMSAConfiguration();
		
		//AuditService
		Services.getAuditService().init();

		//ConfigurationService
		Services.getConfigurationService().init();
		
		//NotificationService
		Services.getNotificationService().init();
		
	}

}
