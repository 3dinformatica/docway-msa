package it.tredi.msa.configuration.docway;

import java.util.List;

/**
 * Configurazione relativa al modulo di rifiuto email in base ad allegati non supportati contenuti al suo interno. Processo di rifiuto
 * documenti di DocWay.
 */
public class RifiutoByAttachmentsConfiguration {

	/**
	 * Abilitazione del modulo
	 */
	private boolean enabled = false;
	
	/**
	 * Elenco di estensioni ammesse dal sistema documentale
	 */
	private List<String> allowedExtensions;
	
	/**
	 * Eventuale codice del fascicolo all'interno del quale deve essere spostato il documento rifiutato
	 */
	private String codFascicolo;
	

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getAllowedExtensions() {
		return allowedExtensions;
	}

	public void setAllowedExtensions(List<String> allowedExtensions) {
		this.allowedExtensions = allowedExtensions;
	}

	public String getCodFascicolo() {
		return codFascicolo;
	}

	public void setCodFascicolo(String codFascicolo) {
		this.codFascicolo = codFascicolo;
	}
	
}
