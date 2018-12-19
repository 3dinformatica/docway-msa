package it.tredi.msa.audit;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import it.tredi.msa.ContextProvider;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.entity.AuditMailboxRun;
import it.tredi.msa.entity.AuditMessage;
import it.tredi.msa.entity.AuditMessageStatus;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.repository.AuditMailboxRunRepository;
import it.tredi.msa.repository.AuditMessageRepository;

public class MongodbAuditWriter extends AuditWriter {
	
	private static final Logger logger = LogManager.getLogger(MongodbAuditWriter.class.getName());
	
	private AuditMessageRepository auditMessageRepository;
	private AuditMailboxRunRepository auditMailboxRunRepository;
	private GridFsOperations gridFsOperations;
	
	private static final String MESSAGGIO_EMAIL_FILENAME = "Messaggio.eml";
	
	public MongodbAuditWriter() {
		super();
		auditMessageRepository = ContextProvider.getBean(AuditMessageRepository.class);
		auditMailboxRunRepository = ContextProvider.getBean(AuditMailboxRunRepository.class);
		gridFsOperations = ContextProvider.getBean(GridFsOperations.class);
	}	

	@Override
	public void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		AuditMessage auditMessage = auditMessageRepository.findByMessageIdAndMailboxName(parsedMessage.getMessageId(), mailboxConfiguration.getName());
		
		if (isFull()) { //full audit -> update or create new audit message collection in mongoDb
			auditMessage = (auditMessage == null)? new AuditMessage() : auditMessage;
			auditMessage.setDate(new Date());
			if (auditMessage.getEmlId() != null) { //delete previous EML (if found)
				gridFsOperations.delete(new Query(Criteria.where("_id").is(new ObjectId(auditMessage.getEmlId()))));
			}			
			auditMessage.setEmlId(null);
			auditMessage.setErrorMessage(null);
			auditMessage.setErrorStackTrace(null);
			auditMessage.setMailboxAddress(mailboxConfiguration.getUser());
			auditMessage.setMailboxName(mailboxConfiguration.getName());
			auditMessage.setMessageId(parsedMessage.getMessageId());
			auditMessage.setSentDate(parsedMessage.getSentDate());
			auditMessage.setStatus(AuditMessageStatus.SUCCESS);
			auditMessage.setSubject(parsedMessage.getSubject());
			auditMessageRepository.save(auditMessage);
		}	
		else { //base audit -> (if found) remove audit message from mongoDb collection
			if (auditMessage != null) {
				if (auditMessage.getEmlId() != null) { //delete previous EML (if found)
					gridFsOperations.delete(new Query(Criteria.where("_id").is(new ObjectId(auditMessage.getEmlId()))));
				}				
				auditMessageRepository.delete(auditMessage);
			}
		}
		
	}

	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception exception) throws Exception {
		AuditMessage auditMessage = auditMessageRepository.findByMessageIdAndMailboxName(parsedMessage.getMessageId(), mailboxConfiguration.getName());
		auditMessage = (auditMessage == null)? new AuditMessage() : auditMessage;
		auditMessage.setDate(new Date());
		
		//store EML
		byte []b = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();			
		ObjectId objId = gridFsOperations.store(new ByteArrayInputStream(b), MESSAGGIO_EMAIL_FILENAME);
		if (auditMessage.getEmlId() != null) { //delete previous EML (if found)
			gridFsOperations.delete(new Query(Criteria.where("_id").is(new ObjectId(auditMessage.getEmlId()))));
		}
		auditMessage.setEmlId(objId.toHexString());
		auditMessage.setErrorMessage(exception.getMessage());
		
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
		try {
			auditMessage.setFromAddress(parsedMessage.getFromAddress());
		}
		catch(Exception e) {
			logger.error("MongodbAuditWriter: Unable to retrieve 'from-address' from message... " + e.getMessage());
		}
		
		auditMessageRepository.save(auditMessage);
	}

	@Override
	public boolean isErrorMessageFoundInAudit(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		return auditMessageRepository.findByMessageIdAndMailboxNameAndStatus(parsedMessage.getMessageId(), mailboxConfiguration.getName(), AuditMessageStatus.ERROR) != null;
	}

	@Override
	public AuditMailboxRun writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception {
		//AuditMailboxRun lastAuditMailboxRun = auditMailboxRunRepository.findByMailboxName(auditMailboxRun.getMailboxName());
		//if (lastAuditMailboxRun != null) //keep only one execution
		//	auditMailboxRunRepository.delete(lastAuditMailboxRun);
		return auditMailboxRunRepository.save(auditMailboxRun);
	}

}
