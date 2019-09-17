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
	 * Eventuale oggetto della mail di rifiuto da inviare al mittente del messaggio
	 */
	private String mailRifiutoSubject;
	
	/**
	 * Eventuale corpo della mail di rifiuto da inviare al mittente del messaggio
	 */
	private String mailRifiutoBody;
	

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

	public String getMailRifiutoSubject() {
		return mailRifiutoSubject;
	}

	public void setMailRifiutoSubject(String mailRifiutoSubject) {
		this.mailRifiutoSubject = mailRifiutoSubject;
	}

	public String getMailRifiutoBody() {
		return mailRifiutoBody;
	}

	public void setMailRifiutoBody(String mailRifiutoBody) {
		this.mailRifiutoBody = mailRifiutoBody;
	}
	
}
