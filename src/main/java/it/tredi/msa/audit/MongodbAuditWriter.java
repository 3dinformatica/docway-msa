package it.tredi.msa.audit;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.mail.Message;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;

import it.tredi.mail.MessageUtils;
import it.tredi.msa.ContextProvider;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.entity.AuditMailboxRun;
import it.tredi.msa.entity.AuditMessage;
import it.tredi.msa.entity.AuditMessageStatus;
import it.tredi.msa.mailboxmanager.MessageContentProvider;
import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.repository.AuditMailboxRunRepository;
import it.tredi.msa.repository.AuditMessageRepository;

/**
 * Implementazione su MongoDB del writer dell'audit sullo stato di elaborazione di MSA
 */
public class MongodbAuditWriter extends AuditWriter {
	
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
			auditMessage.setFromAddress(retrieveFromAddress(parsedMessage));
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
		
		byte[] b = (new MessageContentProvider(parsedMessage.getMessage(), false)).getContent();
		this._writeErrorAuditMessage(
				mailboxConfiguration.getName(), 
				mailboxConfiguration.getUser(), 
				parsedMessage.getMessageId(), 
				parsedMessage.getSentDate(), 
				parsedMessage.getSubject(), 
				retrieveFromAddress(parsedMessage), 
				b, 
				exception);
	}
	
	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, Message message, Exception exception) throws Exception {
		String messageId = MessageUtils.getMessageId(message);
		byte[] b = (new MessageContentProvider(message, false)).getContent();
		
		this._writeErrorAuditMessage(
				mailboxConfiguration.getName(), 
				mailboxConfiguration.getUser(), 
				messageId, 
				message.getSentDate(), 
				message.getSubject(), 
				MessageUtils.getFromAddress(message), 
				b, 
				exception);
	}
	
	/**
	 * Salvataggio (inserimento o aggiornamento) di un record di audit relativo ad un messaggio di posta sul quale e' stato riscontrato
	 * un errore in fase di elaborazione (parsing, trasformazione in documento o salvataggio sul documentale)
	 * @param mailboxName Nome della casella di posta corrente
	 * @param mailboxAddress Indirizzo della casella di posta corrente
	 * @param messageId Identificativo del messaggio sul quale e' stato riscontrato errore
	 * @param sendDate Data di invio del messaggio
	 * @param subject Oggetto del messaggio
	 * @param fromAddress Indirizzo email del mittente del messaggio
	 * @param content Contenuto del messaggio (EML)
	 * @param exception Eccezione riscontrata in fase di elaborazione del messaggio
	 */
	private void _writeErrorAuditMessage(String mailboxName, String mailboxAddress, 
			String messageId, Date sendDate, String subject, 
			String fromAddress, byte[] content, Exception exception) {
		
		AuditMessage auditMessage = auditMessageRepository.findByMessageIdAndMailboxName(messageId, mailboxName);
		auditMessage = (auditMessage == null)? new AuditMessage() : auditMessage;
		auditMessage.setDate(new Date());
		
		//store EML
		ObjectId objId = gridFsOperations.store(new ByteArrayInputStream(content), MESSAGGIO_EMAIL_FILENAME);
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
		auditMessage.setMailboxAddress(mailboxAddress);
		auditMessage.setMailboxName(mailboxName);
		auditMessage.setMessageId(messageId);
		auditMessage.setSentDate(sendDate);
		auditMessage.setStatus(AuditMessageStatus.ERROR);
		auditMessage.setSubject(subject);
		auditMessage.setFromAddress(fromAddress);
		
		auditMessageRepository.save(auditMessage);
	}
	
	@Override
	public boolean isErrorMessageFoundInAudit(MailboxConfiguration mailboxConfiguration, String messageId) throws Exception {
		return auditMessageRepository.findByMessageIdAndMailboxNameAndStatus(messageId, mailboxConfiguration.getName(), AuditMessageStatus.ERROR) != null;
	}

	@Override
	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception {
		//AuditMailboxRun lastAuditMailboxRun = auditMailboxRunRepository.findByMailboxName(auditMailboxRun.getMailboxName());
		//if (lastAuditMailboxRun != null) //keep only one execution
		//	auditMailboxRunRepository.delete(lastAuditMailboxRun);
		auditMailboxRunRepository.save(auditMailboxRun);
	}

}
