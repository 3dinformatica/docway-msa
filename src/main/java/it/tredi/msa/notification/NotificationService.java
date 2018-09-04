package it.tredi.msa.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import it.tredi.msa.ObjectFactory;
import it.tredi.msa.Services;

public class NotificationService {
	
	private static NotificationService instance;
	private NotificationSender notificationSender;
	public final static String NOTIFICATION_ERROR_MESSAGE = "Errore durante la notifica del messaggio: '%s'";
	public final static String NOTIFICATION_ERROR_MESSAGE_DEST = "Errore durante la notifica del messaggio a '%s': '%s'";
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
			boolean sucess = notificationSender.notifyError(message);
			if (!sucess)
				throw new Exception(NOTIFICATION_ERROR_MESSAGE);
		}
		catch (Exception e) {
			logger.error(String.format(NOTIFICATION_ERROR_MESSAGE, message), e);
		}
	}

	public NotificationSender getNotificationSender() {
		return notificationSender;
	}

}



