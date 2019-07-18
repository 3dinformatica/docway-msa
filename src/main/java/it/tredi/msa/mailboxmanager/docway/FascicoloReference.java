package it.tredi.msa.mailboxmanager.docway;

import java.util.List;

/**
 * Informazioni minimali necessari ad identificare un fascicolo 
 */
public class FascicoloReference {

	/**
	 * Codice del fascicolo
	 */
	private String codFascicolo;
	
	/**
	 * Oggetto del fascicolo
	 */
	private String oggetto;
	
	/**
	 * Elenco di rif interni associati al fascicolo
	 */
	private List<RifInterno> rifs;
	
	/**
	 * Codice di classificazione del fascicolo
	 */
	private String codClassif;
	
	/**
	 * Descrizione di classificazione del fascicolo
	 */
	private String descrClassif;
	
	/**
	 * Costruttore vuoto
	 */
	public FascicoloReference() {
	}
	
	/**
	 * Costruttore
	 * @param codFascicolo Codice del fascicolo
	 * @param oggetto Oggetto del fascicolo
	 */
	public FascicoloReference(String codFascicolo, String oggetto) {
		this.codFascicolo = codFascicolo;
		this.oggetto = oggetto;
	}

	public String getCodFascicolo() {
		return codFascicolo;
	}

	public void setCodFascicolo(String codFascicolo) {
		this.codFascicolo = codFascicolo;
	}

	public String getOggetto() {
		return oggetto;
	}

	public void setOggetto(String oggetto) {
		this.oggetto = oggetto;
	}

	public List<RifInterno> getRifs() {
		return rifs;
	}

	public void setRifs(List<RifInterno> rifs) {
		this.rifs = rifs;
	}

	public String getCodClassif() {
		return codClassif;
	}

	public void setCodClassif(String codClassif) {
		this.codClassif = codClassif;
	}

	public String getDescrClassif() {
		return descrClassif;
	}

	public void setDescrClassif(String descrClassif) {
		this.descrClassif = descrClassif;
	}
	
}
