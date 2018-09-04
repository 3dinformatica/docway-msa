package it.tredi.msa.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gestore delle notifiche d'errore di default. Nessuna notifica inviata!
 */
public class DummySender extends NotificationSender {

	private static final Logger logger = LogManager.getLogger(DummySender.class.getName());

	public DummySender() { }

	@Override
	public boolean notifyError(String message) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Notification disabled! Configure 'notification.sender'... " + message);
		return true;
	}
}
