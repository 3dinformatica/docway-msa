package it.tredi.msa.entity;

public class DocwayDocument {
	
	String tipo;
	String oggetto;
	boolean bozza;
	String note;
	String tipologia;
	String mezzo_trasmissione;
	
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
	
	public String getMezzo_trasmissione() {
		return mezzo_trasmissione;
	}
	
	public void setMezzo_trasmissione(String mezzo_trasmissione) {
		this.mezzo_trasmissione = mezzo_trasmissione;
	}

	
}
