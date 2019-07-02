package it.tredi.msa.mailboxmanager.docway.exception;

import java.util.List;

/**
 * Eccezione generata in caso di recupero di piu' fascicoli in base ai TAGS rilevati sull'oggetto del messaggio da processare
 * @author mbernardini
 *
 */
public class MultipleFoldersException extends Exception {

	private static final long serialVersionUID = -7797005431389248774L;
	
	/**
	 * Elenco dei TAGS in base ai quali e' stato ricercato il fascicolo
	 */
	private List<String> tags;
	
	public MultipleFoldersException(List<String> tags) {
		super("Found multiple folders by TAGS " + ((tags != null) ? String.join(", ", tags) : "NULL"));
		this.tags = tags;
	}

	public MultipleFoldersException(List<String> tags, Throwable throwable) {
		super("Found multiple folders by TAGS " + ((tags != null) ? String.join(", ", tags) : "NULL"), throwable);
		this.tags = tags;
	}
	
	public List<String> getTags() {
		return tags;
	}
	
}
