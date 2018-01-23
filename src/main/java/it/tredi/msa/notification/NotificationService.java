package it.tredi.msa.notification;

import it.tredi.msa.ObjectFactory;
import it.tredi.msa.Services;

public class NotificationService {
	
	private static NotificationService instance;
	
	private NotificationSender notificationSender;
	
	private NotificationService() {
	}

	public static synchronized NotificationService getInstance() {
	    if (instance == null) {
	        instance = new NotificationService();
	    }
	    return instance;
	}

	public void init() {
		notificationSender = ObjectFactory.createNotificationSender(Services.getConfigurationService().getMSAConfiguration().getNotificationSenderConfiguration());
	}

	

}



