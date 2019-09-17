package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Informazioni di rifiuto di un documento
 */
public class Rifiuto {

	/**
	 * Operatore che ha effettuato il rifiuto del documento
	 */
	private String operatore;
	
	/**
	 * Codice dell'operatore che ha effettuato il rifiuto del documento
	 */
	private String codOperatore;
	
	/**
	 * Data di rifiuto del documento
	 */
	private String data;
	
	/**
	 * Ora di rifiuto del documento
	 */
	private String ora;
	
	/**
	 * Motivazione del rifiuto
	 */
	private String motivazione;

	
	public String getOperatore() {
		return operatore;
	}

	public void setOperatore(String operatore) {
		this.operatore = operatore;
	}

	public String getCodOperatore() {
		return codOperatore;
	}

	public void setCodOperatore(String codOperatore) {
		this.codOperatore = codOperatore;
	}

	public String getData() {
		return data;
	}

	public void setData(Date date) {
		this.data = new SimpleDateFormat("yyyyMMdd").format(date);
	}

	public String getOra() {
		return ora;
	}

	public void setOra(Date date) {
		this.ora = new SimpleDateFormat("HH:mm:ss").format(date);
	}

	public String getMotivazione() {
		return motivazione;
	}

	public void setMotivazione(String motivazione) {
		this.motivazione = motivazione;
	}
	
}
