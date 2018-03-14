package it.tredi.msa.entity.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.tredi.msa.entity.ContentProvider;

public class DocwayFile {
	
	private String id;
	private String name;
	
	//checkin info
	private String operatore;
	private String codOperatore;
	private String data;
	private String ora;
	
	//content
	private ContentProvider contentProvider;

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

	public ContentProvider getContentProvider() {
		return contentProvider;
	}

	public void setContentProvider(ContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public byte []getContent() throws Exception {
		return contentProvider.getContent();
	}
	
}
