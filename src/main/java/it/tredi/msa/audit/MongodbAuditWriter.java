package it.tredi.msa.audit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import it.tredi.msa.ContextProvider;
import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;

public class MongodbAuditWriter extends AuditWriter {
	
	private AuditMessageRepository auditMessageRepository;
	private AuditMailboxRunRepository auditMailboxRunRepository;
	
	public MongodbAuditWriter() {
		super();
		auditMessageRepository = ContextProvider.getBean(AuditMessageRepository.class);
		auditMailboxRunRepository = ContextProvider.getBean(AuditMailboxRunRepository.class);
	}	

	@Override
	public void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		AuditMessage auditMessage = auditMessageRepository.findByMessageIdAndMailboxName(parsedMessage.getMessageId(), mailboxConfiguration.getName());
		
		if (isFull()) { //full audit -> update or create new audit message collection in mongoDb
			auditMessage = (auditMessage == null)? new AuditMessage() : auditMessage;
			auditMessage.setDate(new Date());
			auditMessage.setEmlId(null);
			auditMessage.setErrorMessge(null);
			auditMessage.setErrorStackTrace(null);
			auditMessage.setMailboxAddress(mailboxConfiguration.getUser());
			auditMessage.setMailboxName(mailboxConfiguration.getName());
			auditMessage.setMessageId(parsedMessage.getMessageId());
			auditMessage.setSentDate(parsedMessage.getSentDate());
			auditMessage.setStatus(AuditMessageStatus.SUCCESS);
			auditMessage.setSubject(parsedMessage.getSubject());
			auditMessageRepository.save(auditMessage);
		}	
		else { //base audit -> (if found) remove audit message collection from mongoDb
			if (auditMessage != null)
				auditMessageRepository.delete(auditMessage);
		}
		
//TODO - MANCA gestione EML
	}

	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception exception) throws Exception {
		AuditMessage auditMessage = auditMessageRepository.findByMessageIdAndMailboxName(parsedMessage.getMessageId(), mailboxConfiguration.getName());
		auditMessage = (auditMessage == null)? new AuditMessage() : auditMessage;
		auditMessage.setDate(new Date());
		auditMessage.setEmlId(null);
		auditMessage.setErrorMessge(exception.getMessage());
		
		//stack trace to string
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String sStackTrace = sw.toString();
		
		auditMessage.setErrorStackTrace(sStackTrace);
		auditMessage.setMailboxAddress(mailboxConfiguration.getUser());
		auditMessage.setMailboxName(mailboxConfiguration.getName());
		auditMessage.setMessageId(parsedMessage.getMessageId());
		auditMessage.setSentDate(parsedMessage.getSentDate());
		auditMessage.setStatus(AuditMessageStatus.ERROR);
		auditMessage.setSubject(parsedMessage.getSubject());
		auditMessageRepository.save(auditMessage);
		
//TODO - manca gestione EML
	}

	@Override
	public boolean auditMessageInErrorFound(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		return auditMessageRepository.findByMessageIdAndMailboxNameAndStatus(parsedMessage.getMessageId(), mailboxConfiguration.getName(), AuditMessageStatus.ERROR) != null;
	}

	@Override
	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception {
		AuditMailboxRun lastAuditMailboxRun = auditMailboxRunRepository.findByMailboxName(auditMailboxRun.getMailboxName());
		if (lastAuditMailboxRun != null) //keep only one execution
			auditMailboxRunRepository.delete(lastAuditMailboxRun);
		auditMailboxRunRepository.save(auditMailboxRun);
	}

}
