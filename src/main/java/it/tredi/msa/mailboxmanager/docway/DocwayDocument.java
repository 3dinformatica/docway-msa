package it.tredi.msa.mailboxmanager.docway;

import java.util.ArrayList;
import java.util.List;

import it.tredi.msa.mailboxmanager.docway.fatturapa.FatturaPAItem;

/**
 * Classe di model che identifica tutte le informazioni relative ad un documento (le cui parti vengono compilate con dati estratti dai 
 * messaggi email)
 */
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
	private String repertorioNum;
	private List<StoriaItem> storia;
	private List<RifEsterno> rifEsterni;
	private List<RifInterno> rifInterni;
	private List<DocwayFile> files;
	private List<DocwayFile> immagini;
	private List<String> allegato;
	private List<Postit> postitL;
	
	//fatturaPA
	private FatturaPAItem fatturaPA;
	
	public DocwayDocument() {
		this.storia = new ArrayList<StoriaItem>();
		this.rifEsterni = new ArrayList<RifEsterno>();
		this.rifInterni = new ArrayList<RifInterno>();
		this.files = new ArrayList<DocwayFile>();
		this.immagini = new ArrayList<DocwayFile>();
		this.allegato = new ArrayList<String>();
		this.postitL = new ArrayList<Postit>();
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

	public String getRepertorioNum() {
		return repertorioNum;
	}

	public void setRepertorioNum(String repertorioNum) {
		this.repertorioNum = repertorioNum;
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

	public List<RifInterno> getRifInterni() {
		return rifInterni;
	}

	public void setRifInterni(List<RifInterno> rifInterni) {
		this.rifInterni = rifInterni;
	}
	
	public void addRifInterno(RifInterno rifInterno) {
		//check duplicati
		RifInterno rif = getRifInterno(rifInterno);
		if (rif == null) {
			rifInterno.setNotify(true); //send notify email only for new rifs (avoid duplicates)
			rifInterni.add(rifInterno);
		}
		else
			updateRifInterno(rif, rifInterno);
	}
	
	private RifInterno getRifInterno(RifInterno rifInterno) {
		for (RifInterno rif:rifInterni)
			if (rif.getDiritto().equals(rifInterno.getDiritto()) && rif.getCodPersona().equals(rifInterno.getCodPersona()) && rif.getCodUff().equals(rifInterno.getCodUff())) 
				return rif;
		return null;
	}
	
	private void updateRifInterno(RifInterno rifToUpdate, RifInterno rifInterno) {
		 if (!rifToUpdate.isIntervento() && rifInterno.isIntervento())
			 rifToUpdate.setIntervento(true);
	}

	public List<DocwayFile> getFiles() {
		return files;
	}

	public void setFiles(List<DocwayFile> files) {
		this.files = files;
	}

	public void addFile(DocwayFile file) {
		files.add(file);
	}	

	public void addFile(int index, DocwayFile file) {
		files.add(index, file);
	}
	
	public List<DocwayFile> getImmagini() {
		return immagini;
	}

	public void setImmagini(List<DocwayFile> immagini) {
		this.immagini = immagini;
	}

	public void addImmagine(DocwayFile file) {
		immagini.add(file);
	}

	public List<String> getAllegato() {
		return allegato;
	}

	public void setAllegato(List<String> allegato) {
		this.allegato = allegato;
	}
	
	public void addAllegato(String descrizione_allegato) {
		allegato.add(descrizione_allegato);
	}

	public List<Postit> getPostitL() {
		return postitL;
	}

	public void setPostit(List<Postit> postitL) {
		this.postitL = postitL;
	}
	
	public void addPostit(Postit postit) {
		postitL.add(postit);
	}

	public FatturaPAItem getFatturaPA() {
		return fatturaPA;
	}

	public void setFatturaPA(FatturaPAItem fatturaPA) {
		this.fatturaPA = fatturaPA;
	}
	
}
