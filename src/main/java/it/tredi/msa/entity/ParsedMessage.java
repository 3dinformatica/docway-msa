package it.tredi.msa.entity;

import javax.mail.Message;

import it.tredi.mail.MessageUtils;

public class ParsedMessage {
	
	private Message message;

	public ParsedMessage(Message message) {
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	public String getSubject() {
		return MessageUtils.getSubject(message);
	}

}
