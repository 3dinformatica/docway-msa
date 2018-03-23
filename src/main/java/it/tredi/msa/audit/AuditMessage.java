package it.tredi.msa.audit;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndexes({
    @CompoundIndex(name = "messageId_mailboxName_idx", def = "{'messageId': 1, 'mailboxName': 1}", unique = true)
})
public class AuditMessage {
	
	@Id
	private String id;

	@Indexed
	private String mailboxName;
	
	@Indexed
	private String messageId;

	@Indexed
	private Date sentDate;
	
	private String subject;
	
	@Indexed
	private AuditMessageStatus status;
	
	private String errorMessge;
	
	private String errorStackTrace;
	
	@Indexed
	private String mailboxAddress;
	
	private String emlId;
	
	@Indexed
	private Date date;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMailboxName() {
		return mailboxName;
	}

	public void setMailboxName(String mailboxName) {
		this.mailboxName = mailboxName;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public AuditMessageStatus getStatus() {
		return status;
	}

	public void setStatus(AuditMessageStatus status) {
		this.status = status;
	}

	public String getErrorMessge() {
		return errorMessge;
	}

	public void setErrorMessge(String errorMessge) {
		this.errorMessge = errorMessge;
	}

	public String getErrorStackTrace() {
		return errorStackTrace;
	}

	public void setErrorStackTrace(String errorStackTrace) {
		this.errorStackTrace = errorStackTrace;
	}

	public String getMailboxAddress() {
		return mailboxAddress;
	}

	public void setMailboxAddress(String mailboxAddress) {
		this.mailboxAddress = mailboxAddress;
	}

	public String getEmlId() {
		return emlId;
	}

	public void setEmlId(String emlId) {
		this.emlId = emlId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
}
