package it.tredi.msa.mailboxmanager.docway;

import java.text.SimpleDateFormat;
import java.util.Date;

import it.tredi.msa.mailboxmanager.ContentProvider;

public class InteroperabilitaItem {
	
	private String id;
	private String name;
	private String info;
	private String data;
	private String ora;
	private String messageId;
	
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

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
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

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
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
