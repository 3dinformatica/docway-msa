package it.tredi.msa.entity;

import java.util.Date;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

import it.tredi.mail.MessageUtils;

public class ParsedMessage {
	
	private Message message;
	private String messageId;
	
	public ParsedMessage(Message message) {
		this.message = message;
		this.messageId = null;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	public String getSubject() throws MessagingException {
		String subject = message.getSubject();
		if (subject == null || subject.isEmpty())
			subject = "[NESSUN OGGETTO]";
		return subject;
	}
	
	public String getFromAddress() throws MessagingException {
		return MessageUtils.getFromAddress(message);
	}

	public String getFromPersonal() throws MessagingException {
		return MessageUtils.getFromPersonal(message);
	}

	public Date getSentDate() throws MessagingException {
		return message.getSentDate();
	}
	
	public String getMessageId() throws Exception {
		if (messageId == null) {
			messageId = MessageUtils.getMessageId(message);
    		messageId = messageId.replaceAll("<", "");
    		messageId = messageId.replaceAll(">", "");
		}
		return messageId;
	}
	
}
