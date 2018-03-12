package it.tredi.msa.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import it.tredi.mail.MessageUtils;

public class ParsedMessage {
	
	private Message message;
	private String messageId;
	private List<Part> leafPartsL;
	private List<Part> attachmentsL;
	private String textParts;
	private String htmlParts;
	
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
	
	public String getToAddressesAsString() throws Exception {
		return stringArrayToString(MessageUtils.getToAddresses(message));
	}
	
	public String getCcAddressesAsString() throws Exception {
		return stringArrayToString(MessageUtils.getCcAddresses(message));
	}
	
	private String stringArrayToString(String []array) throws Exception {
		String ret = "";
		for (String entry:array)
			ret += ", " + entry;
		if (ret.length() >= 2)
			return ret.substring(2);
		else
			return "";
	}	
	
	public List<Part> getLeafPartsL() throws MessagingException, IOException {
		if (leafPartsL == null)	
			leafPartsL = MessageUtils.getLeafParts(message);
		return leafPartsL;
	}
	
	public List<Part> getAttachments() throws MessagingException, IOException {
		if (attachmentsL == null) {
			attachmentsL = new ArrayList<Part>();
			for (Part part:getLeafPartsL())
				if (MessageUtils.isAttachmentPart(part))
					attachmentsL.add(part);
		}
		return attachmentsL;
	}
	
	public String getTextParts() throws MessagingException, IOException {
		if (textParts == null) {
			textParts = "";
			for (Part part:getLeafPartsL()) 
				if (MessageUtils.isTextPart(part))			
					textParts += part.getContent();
		}
		return textParts;
	}

	public String getHtmlParts() throws MessagingException, IOException {
		if (htmlParts == null) {
			htmlParts = "";
			for (Part part:getLeafPartsL()) 
				if (MessageUtils.isHtmlPart(part))			
					htmlParts += part.getContent();
		}
		return htmlParts;
	}
	
}
