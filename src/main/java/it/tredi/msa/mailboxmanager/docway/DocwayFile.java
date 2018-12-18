package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.tredi.msa.mailboxmanager.ContentProvider;

/**
 * Classe di model utilizzata per identificare un file allegato al documento (es. allegato della mail estratto e caricato sul documento)
 */
public class DocwayFile {
	
	private String id;
	private String name;
	
	//checkin info
	private String operatore;
	private String codOperatore;
	private String data;
	private String ora;
	
	//content
	private byte[] content;
	
	private boolean fromFatturaPA = false;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	public void setData(String data) {
		this.data = data;
	}
	
	public void setData(Date date) {
		this.data = new SimpleDateFormat("yyyyMMdd").format(date);
	}	

	public String getOra() {
		return ora;
	}

	public void setOra(String ora) {
		this.ora = ora;
	}
	
	public void setOra(Date date) {
		this.ora = new SimpleDateFormat("HH:mm:ss").format(date);
	}	

	public byte[] getContent() {
		return content;
	}
	
	public void setContent(byte[] content) {
		this.content = content;
	}

	public void setContentByProvider(ContentProvider contentProvider) throws Exception {
		this.content = contentProvider.getContent();
	}

	public boolean isFromFatturaPA() {
		return fromFatturaPA;
	}

	public void setFromFatturaPA(boolean fromFatturaPA) {
		this.fromFatturaPA = fromFatturaPA;
	}
	
}
