package it.tredi.msa.mailboxmanager.docway.exception;

/**
 * Eccezione generata in caso di recupero di piu' fascicoli in base ad una query per la quale era previsto il caricamento di uno ed un
 * solo fascicolo
 */
public class MultipleFoldersException extends Exception {

	private static final long serialVersionUID = -7797005431389248774L;
	
	/**
	 * Query utilizzata per il caricamento del fascicolo
	 */
	private String query;
	
	/**
	 * Numero di fascicoli trovati in base alla query specificata
	 */
	private int elements;
	
	/**
	 * Costruttore
	 * @param query Query utilizzata per il caricamento del fascicolo
	 * @param elements Numero di fascicoli trovati in base alla query specificata
	 */
	public MultipleFoldersException(String query, int elements) {
		super("Found " + elements + " folders by QUERY: " + query);
		this.query = query;
		this.elements = elements;
	}

	/**
	 * Costruttore
	 * @param query Query utilizzata per il caricamento del fascicolo
	 * @param elements Numero di fascicoli trovati in base alla query specificata
	 * @param throwable
	 */
	public MultipleFoldersException(String query, int elements, Throwable throwable) {
		super("Found " + elements + " folders by QUERY: " + query, throwable);
		this.query = query;
		this.elements = elements;
	}
	
	/**
	 * Ritorna la query utilizzata per il recupero del fascicolo
	 * @return
	 */
	public String getQuery() {
		return this.query;
	}
	
	/**
	 * Ritorna il numero di fascicoli trovati in base alla query specificata
	 * @return
	 */
	public int getElements() {
		return this.elements;
	}
	
}
