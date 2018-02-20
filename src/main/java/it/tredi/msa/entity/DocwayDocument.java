package it.tredi.msa.entity;

public class DocwayDocument {
	
	private String tipo;
	private String oggetto;
	boolean bozza;
	private String note;
	private String tipologia;
	private String mezzoTrasmissione;
	private String codAmmAoo;
	private String anno;
	private String dataProt;
	private String numProt;
	private boolean annullato;
	private String messageId;
	private String recipientEmail;
	private String classif;
	private String classifCod;
	//private StoriaItem creazione;
	
	public String getTipo() {
		return tipo;
	}
	
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	
	public String getOggetto() {
		return oggetto;
	}
	
	public void setOggetto(String oggetto) {
		this.oggetto = oggetto;
	}
	
	public boolean isBozza() {
		return bozza;
	}
	
	public void setBozza(boolean bozza) {
		this.bozza = bozza;
	}
	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
	}
	
	public String getTipologia() {
		return tipologia;
	}
	
	public void setTipologia(String tipologia) {
		this.tipologia = tipologia;
	}

	public String getMezzoTrasmissione() {
		return mezzoTrasmissione;
	}

	public void setMezzoTrasmissione(String mezzoTrasmissione) {
		this.mezzoTrasmissione = mezzoTrasmissione;
	}

	public String getCodAmmAoo() {
		return codAmmAoo;
	}

	public void setCodAmmAoo(String codAmmAoo) {
		this.codAmmAoo = codAmmAoo;
	}

	public String getAnno() {
		return anno;
	}

	public void setAnno(String anno) {
		this.anno = anno;
	}

	public boolean isAnnullato() {
		return annullato;
	}

	public void setAnnullato(boolean annullato) {
		this.annullato = annullato;
	}

	public String getDataProt() {
		return dataProt;
	}

	public void setDataProt(String dataProt) {
		this.dataProt = dataProt;
	}

	public String getNumProt() {
		return numProt;
	}

	public void setNumProt(String numProt) {
		this.numProt = numProt;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public void setRecipientEmail(String recipientEmail) {
		this.recipientEmail = recipientEmail;
	}

	public String getClassif() {
		return classif;
	}

	public void setClassif(String classif) {
		this.classif = classif;
	}

	public String getClassifCod() {
		return classifCod;
	}

	public void setClassifCod(String classifCod) {
		this.classifCod = classifCod;
	}
	
}
