package it.tredi.extraway;

import org.dom4j.Document;

/**
 * Documento lockato recuperato da eXtraWay
 */
public class LockedDocument {

	/**
	 * Documento recuperato da eXtraWay
	 */
	private Document doc;
	
	/**
	 * Codice di LOCK restituito da eXtraWay
	 */
	private String theLock;
	
	/**
	 * Costruttore
	 * @param doc Documento recuperato
	 * @param theLock Codice di LOCK assegnato
	 */
	public LockedDocument(Document doc, String theLock) {
		this.doc = doc;
		this.theLock = theLock;
	}

	public Document getDoc() {
		return doc;
	}

	public String getTheLock() {
		return theLock;
	}
	
}
