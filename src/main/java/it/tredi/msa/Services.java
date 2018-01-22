package it.tredi.msa;

import javax.management.Notification;

import ir.tredi.msa.configuration.ConfigurationService;
import it.tredi.msa.notification.NotificationService;

public class Services {
	
	public static ConfigurationService getConfigurationService() {
		return ConfigurationService.getInstance();
	}
	
	public static NotificationService getNotificationService() {
		return NotificationService.getInstance();
	}

}
