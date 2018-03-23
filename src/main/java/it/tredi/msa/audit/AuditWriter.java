package it.tredi.msa.audit;

import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;

public abstract class AuditWriter {
	
	private boolean full;
	
	public abstract void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception;
	public abstract void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception e) throws Exception;
	public abstract boolean auditMessageInErrorFound(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception;
	public abstract void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception;

	public boolean isFull() {
		return full;
	}

	public void setFull(String full) {
		this.setFull(Boolean.parseBoolean(full));
	}	
	
	public void setFull(boolean full) {
		this.full = full;
	}
	
}
