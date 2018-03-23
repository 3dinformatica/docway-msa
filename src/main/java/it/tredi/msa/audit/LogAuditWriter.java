package it.tredi.msa.audit;

import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;

public class LogAuditWriter extends AuditWriter {

	@Override
	public void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		throw new Exception("Log Audit Writer - not yet implemented [writeSuccessAuditMessage()]");
	}

	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception exception) throws Exception {
		throw new Exception("Log Audit Writer - not yet implemented [writeErrorAuditMessage()]");
	}

	@Override
	public boolean auditMessageInErrorFound(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		throw new Exception("Log Audit Writer - not yet implemented [auditMessageInErrorFound()]");		
	}

	@Override
	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception {
		throw new Exception("Log Audit Writer - not yet implemented [writeAuditMailboxRun()]");		
	}

}
