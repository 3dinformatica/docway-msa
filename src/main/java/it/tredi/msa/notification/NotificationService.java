package it.tredi.msa.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.MsaLauncher;
import it.tredi.msa.ObjectFactory;
import it.tredi.msa.Services;

public class NotificationService {
	
	private static NotificationService instance;
	private NotificationSender notificationSender;
	private final static String NOTIFICATION_ERROR_MESSAGE = "Errore durante la notifica del messaggio";
	private static final Logger logger = LogManager.getLogger(NotificationService.class.getName());
	
	private NotificationService() {
	}

	public static synchronized NotificationService getInstance() {
	    if (instance == null) {
	        instance = new NotificationService();
	    }
	    return instance;
	}

	public void init() throws Exception {
		notificationSender = ObjectFactory.createNotificationSender(Services.getConfigurationService().getMSAConfiguration().getNotificationSenderConfiguration());
	}

	public void notifyError(String message) {
		try {
			notificationSender.notifiyError(message);	
		}
		catch (Exception e) {
			logger.error(NOTIFICATION_ERROR_MESSAGE + ": " + message, e);
		}
	}

}



