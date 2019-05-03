package it.tredi.msa.mailboxmanager;

import javax.mail.Message;

import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;

public class MessageParserThreadWorkObj {
	
	private int messageIndex;
	private MessageParserRetObjType type;
	private Exception exception;
	private Message message;
	private DocwayParsedMessage parsedMessage;
	private int messageCount;
	private String mailboxAddress;
	
	public int getMessageIndex() {
		return messageIndex;
	}
	
	public MessageParserRetObjType getType() {
		return type;
	}
	
	public Exception getException() {
		return exception;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public DocwayParsedMessage getParsedMessage() {
		return parsedMessage;
	}
	
	public int getMessageCount() {
		return messageCount;
	}
	
	public String getMailboxAddress() {
		return mailboxAddress;
	}

	public MessageParserThreadWorkObj(int messageIndex, Message message, int messageCount, String mailboxAddress) {
		super();
		this.type = MessageParserRetObjType.TODO;
		this.messageIndex = messageIndex;
		this.message = message;
		this.messageCount = messageCount;
		this.mailboxAddress = mailboxAddress;
	}
	
	public void setDONE(DocwayParsedMessage parsedMessage) {
		this.type = MessageParserRetObjType.DONE;
		this.parsedMessage = parsedMessage;
	}
	
	public void setERROR(Exception e) {
		this.type = MessageParserRetObjType.ERROR;
		this.exception = e;
	}
	
	public void setPROCESSED() {
		this.type = MessageParserRetObjType.PROCESSED;
	}

	public boolean isDONE() {
		return type == MessageParserRetObjType.DONE;
	}
	
	public boolean isERROR() {
		return type == MessageParserRetObjType.ERROR;
	}
}

enum MessageParserRetObjType {
	TODO,
	DONE,
	ERROR,
	PROCESSED;
}
