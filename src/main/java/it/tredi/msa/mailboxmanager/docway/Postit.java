package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Postit {
	
	private String text;
	private String operatore;
	private String codOperatore;
	private String data;
	private String ora;
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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
	
	public void setData(Date date) {
		this.data = new SimpleDateFormat("yyyyMMdd").format(date);
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public String getOra() {
		return ora;
	}
	
	public void setOra(Date date) {
		this.ora = new SimpleDateFormat("HH:mm:ss").format(date);
	}	
	
	public void setOra(String ora) {
		this.ora = ora;
	}
	
}
