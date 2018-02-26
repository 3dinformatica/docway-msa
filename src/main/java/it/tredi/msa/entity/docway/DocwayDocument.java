package it.tredi.msa.entity.docway;

import java.util.ArrayList;
import java.util.List;

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
	private String autore;
	private String voceIndice;
	private String repertorio;
	private String repertorioCod;
	private List<StoriaItem> storia;
	private List<RifEsterno> rifEsterni;
	
	public DocwayDocument() {
		this.storia = new ArrayList<StoriaItem>();
		this.rifEsterni = new ArrayList<RifEsterno>();
	}

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

	public String getAutore() {
		return autore;
	}

	public void setAutore(String autore) {
		this.autore = autore;
	}
	
	public String getVoceIndice() {
		return voceIndice;
	}

	public void setVoceIndice(String voceIndice) {
		this.voceIndice = voceIndice;
	}

	public String getRepertorio() {
		return repertorio;
	}

	public void setRepertorio(String repertorio) {
		this.repertorio = repertorio;
	}

	public String getRepertorioCod() {
		return repertorioCod;
	}

	public void setRepertorioCod(String repertorioCod) {
		this.repertorioCod = repertorioCod;
	}

	public List<StoriaItem> getStoria() {
		return storia;
	}

	public void setStoria(List<StoriaItem> storia) {
		this.storia = storia;
	}
	
	public void addStoriaItem(StoriaItem storiaItem) {
		storia.add(storiaItem);
	}

	public List<RifEsterno> getRifEsterni() {
		return rifEsterni;
	}

	public void setRifEsterni(List<RifEsterno> rifEsterni) {
		this.rifEsterni = rifEsterni;
	}
	
	public void addRifEsterno(RifEsterno rifEsterno) {
		rifEsterni.add(rifEsterno);
	}
	
}
