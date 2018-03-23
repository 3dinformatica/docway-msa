package it.tredi.msa.audit;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class AuditMailboxRun {
	
	@Id
	private String id;

	@Indexed (unique=true)
	private String mailboxName;

	@Indexed
	private String mailboxAddress;	
	
	@Indexed
	private Date startDate;

	@Indexed
	private Date endDate;	
	
	@Indexed
	private AuditMailboxRunStatus status;
	
	private String errorMessge;
	
	private String errorStackTrace;
	
	private int messageCount;
	
	private int errorCount;
	
	private int storedCount;
	
	private int newErrorCount;

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

	public String getMailboxAddress() {
		return mailboxAddress;
	}

	public void setMailboxAddress(String mailboxAddress) {
		this.mailboxAddress = mailboxAddress;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public AuditMailboxRunStatus getStatus() {
		return status;
	}

	public void setStatus(AuditMailboxRunStatus status) {
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

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

	public int getNewErrorCount() {
		return newErrorCount;
	}

	public void setNewErrorCount(int newErrorCount) {
		this.newErrorCount = newErrorCount;
	}
	
	public int getStoredCount() {
		return storedCount;
	}

	public void setStoredCount(int storedCount) {
		this.storedCount = storedCount;
	}

	public AuditMailboxRun() {
		this.messageCount = 0;
		this.errorCount = 0;
		this.newErrorCount = 0;
		this.storedCount = 0;
	}
	
	public void incrementErrorCount() {
		this.errorCount++;
	}
	
	public void incrementNewErrorCount() {
		this.newErrorCount++;
	}

	public void incrementStoredCount() {
		this.storedCount++;
	}	
	
}
