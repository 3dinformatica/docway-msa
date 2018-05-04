package it.tredi.msa.mailboxmanager.docway.fatturapa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class NotificaItem {

	private String name;
	private String title;
	private String tipo;
	private String data;
	private String ora;
	private String info;
	private String numeroFattura;
	private String annoFattura;
	private String messageId;	
	private String esito;
	private String note;
	private List<ErroreItem> errori;
	private String riferita;
	
	public NotificaItem() {
		errori = new ArrayList<>();
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTipo() {
		return tipo;
	}
	
	public void setTipo(String tipo) {
		this.tipo = tipo;
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
	
	public String getInfo() {
		return info;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getNumeroFattura() {
		return numeroFattura;
	}
	
	public void setNumeroFattura(String numeroFattura) {
		this.numeroFattura = numeroFattura;
	}
	
	public String getAnnoFattura() {
		return annoFattura;
	}
	
	public void setAnnoFattura(String annoFattura) {
		this.annoFattura = annoFattura;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public String getEsito() {
		return esito;
	}
	
	public void setEsito(String esito) {
		this.esito = esito;
	}
	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
	}
	
	public List<ErroreItem> getErrori() {
		return errori;
	}
	
	public void setErrori(List<ErroreItem> errori) {
		this.errori = errori;
	}
	
	public void addErrore(ErroreItem erroreItem) {
		errori.add(erroreItem);
	}
	
	public String getRiferita() {
		return riferita;
	}

	public void setRiferita(String riferita) {
		this.riferita = riferita;
	}

}
