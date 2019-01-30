package it.tredi.msa.audit;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton di mantenimento in memoria dei messaggi email sui quali e' stato riscontrato errore
 */
public class ErrorMessagesLogAudit {

	private static final Logger logger = LoggerFactory.getLogger(ErrorMessagesLogAudit.class);
	
	private List<String> errors;
	
	// Singleton
	private static ErrorMessagesLogAudit instance = null;

	/**
	 * Istanzia la lista degli errori di elaborazione
	 * @return
	 */
	public static ErrorMessagesLogAudit getInstance() {
		if (instance == null) {
			synchronized (ErrorMessagesLogAudit.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("ErrorMessagesLogAudit instance is null... create one");
					instance = new ErrorMessagesLogAudit();
				}
			}
		}

		return instance;
	}
	
	/**
	 * Costruttore privato
	 */
	private ErrorMessagesLogAudit() {
		this.errors = new ArrayList<String>();
	}
	
	/**
	 * Costruzione della chiave per il mantenimento in memoria della lista
	 * @param mailboxName
	 * @param messageId
	 * @return
	 */
	private String _getKey(String mailboxName, String messageId) {
		return (mailboxName != null && !mailboxName.isEmpty() && messageId != null && !messageId.isEmpty()) ? mailboxName + "_" + messageId : null;
	}
	
	/**
	 * Verifica se il messageId indicato risulta gia' fra quelli per i quali e' stato riscontrato errore
	 * @param mailboxName
	 * @param messageId
	 * @return
	 */
	public boolean containsError(String mailboxName, String messageId) {
		String key = _getKey(mailboxName, messageId);
		return (key != null) ? this.errors.contains(key) : false;
	}
	
	/**
	 * Aggiunta di un messageId alla lista di errori riscontrati
	 * @param mailboxName
	 * @param messageId
	 */
	public void addError(String mailboxName, String messageId) {
		String key = _getKey(mailboxName, messageId);
		if (key != null)
			this.errors.add(key);
	}
	
	/**
	 * Eliminazione di un messageId dalla lista di errori in memoria
	 * @param messageId
	 */
	public void removeError(String mailboxName, String messageId) {
		if (containsError(mailboxName, messageId))
			this.errors.remove(_getKey(mailboxName, messageId));
	}
	
}
