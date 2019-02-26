package it.tredi.msa;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.mailboxmanager.MailboxManager;

/**
 * Mappa contenente i manager (con relative configurazioni) di tutte le caselle di posta elettronica gestite
 * tramite archiviatore
 */
public class MailboxesManagersMap {

	private static final Logger logger = LogManager.getLogger(MailboxesManagersMap.class.getName());
	
	/**
	 * Mappa di manager di caselle di posta gestiti tramite archiviatore. La chiave della mappa corrisponde
	 * all'indirizzo email della casella di posta (username utilizzato per l'accesso)
	 */
	private Map<String, MailboxManager> map;
	
	// Singleton
	private static MailboxesManagersMap instance = null;

	/**
	 * Istanzia la mappa di manager di mailbox (se non gia' istanziata in precedenza)
	 * @return
	 */
	public static MailboxesManagersMap getInstance() {
		if (instance == null) {
			synchronized (MailboxesManagersMap.class) {
				if (instance == null) {
					if (logger.isInfoEnabled())
						logger.info("MailboxesManagersMap instance is null... create one");
					instance = new MailboxesManagersMap();
				}
			}
		}

		return instance;
	}
	
	/**
	 * Costruttore privato
	 */
	private MailboxesManagersMap() {
		this.map = new ConcurrentHashMap<>();
	}
	
	/**
	 * Verifica se il manager indicato risulta gia' in memoria o meno
	 * @param emailAddress Indirizzo email della mailbox da ricercare
	 * @return true se il manager e' registrato in memoria, false altrimenti
	 */
	private boolean containsManager(String emailAddress) {
		return (emailAddress != null) ? this.map.containsKey(emailAddress) : false;
	}
	
	/**
	 * Aggiunta di un manager di mailbox alla mappa in memoria
	 * @param manager Manager da settare
	 */
	protected void addManager(MailboxManager manager) {
		String key = null;
		if (manager != null && manager.getConfiguration() != null)
			key = manager.getConfiguration().getUser();
		if (key != null && !key.isEmpty())
			this.map.put(key, manager);
	}
	
	/**
	 * Recupero di un manager di mailbox dalla memoria
	 * @param emailAddress Indirizzo email della mailbox per la quale si deve recuperare il manager
	 * @return
	 */
	public MailboxManager getManager(String emailAddress) {
		return (emailAddress != null && containsManager(emailAddress)) ? this.map.get(emailAddress) : null;
	}
	
	/**
	 * Ritorna la mappa completa di tutti i manager di mailbox
	 * @return
	 */
	protected Map<String, MailboxManager> getMap() {
		return this.map;
	}
	
	/**
	 * Eliminazione di un manager di mailbox dato l'indirizzo email della casella di posta
	 * @param emailAddress Indirizzo email della mailbox per la quale si deve eliminare il manager 
	 */
	protected void removeManager(String emailAddress) {
		if (containsManager(emailAddress))
			this.map.remove(emailAddress);
	}
	
}
