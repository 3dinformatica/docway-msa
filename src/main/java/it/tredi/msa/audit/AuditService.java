package it.tredi.msa.audit;

import it.tredi.msa.entity.AuditMailboxRun;

import javax.mail.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MessageUtils;
import it.tredi.msa.ObjectFactory;
import it.tredi.msa.Services;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.mailboxmanager.ParsedMessage;

public class AuditService {
	
	private static AuditService instance;
	private AuditWriter auditWriter;
	private static final Logger logger = LogManager.getLogger(AuditService.class.getName());
	
	public final static String WRITE_AUDIT_MESSAGE_LOG_ERROR_MESSAGE = "Error writing audit message [%s] [%s]";
	public final static String WRITE_AUDIT_MESSAGE_MAIL_ERROR_MESSAGE = "Errore imprevisto in fase di registrazione dell'audit per un messaggio di posta [%s]\nConsultare il log per maggiori dettagli.\n\n%s";
	public final static String WRITE_AUDIT_MESSAGE_LOG_MESSAGE = "[%s] writing audit [message] [%s] [%s]. Audit level [%s]";
	public final static String WRITE_AUDIT_MAILBOX_RUN_LOG_MESSAGE = "[%s] writing audit [mailbox run] [%s]";
	public final static String WRITE_AUDIT_MAILBOX_RUN_LOG_ERROR_MESSAGE = "Error writing audit mailbox run [%s]";
	public final static String WRITE_AUDIT_MAILBOX_RUN_MAIL_ERROR_MESSAGE = "Errore imprevisto in fase di registrazione dell'audit per una esecuzione di una casella di posta [%s]\nConsultare il log per maggiori dettagli.\n\n%s";
	
	private String level;
	
	private AuditService() {
	}

	public static synchronized AuditService getInstance() {
	    if (instance == null) {
	        instance = new AuditService();
	    }
	    return instance;
	}

	public void init() throws Exception {
		auditWriter = ObjectFactory.createAuditWriter(Services.getConfigurationService().getMSAConfiguration().getAuditWriterConfiguration());
		level = auditWriter.isFull()? "FULL" : "BASE";
	}

	/**
	 * Scrittura sull'audit di MSA di un messaggio email processato correttamente
	 * @param mailboxConfiguration
	 * @param parsedMessage
	 * @throws Exception
	 */
	public void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		try {
			logger.info(String.format(WRITE_AUDIT_MESSAGE_LOG_MESSAGE, mailboxConfiguration.getName(), parsedMessage.getMessageId(), "SUCCESS", level));
			auditWriter.writeSuccessAuditMessage(mailboxConfiguration, parsedMessage);
		}
		catch (Exception e) {
			logger.error(String.format(WRITE_AUDIT_MESSAGE_LOG_ERROR_MESSAGE, parsedMessage.getMessageId(), "success"), e);
			Services.getNotificationService().notifyError(String.format(WRITE_AUDIT_MESSAGE_MAIL_ERROR_MESSAGE, parsedMessage.getMessageId(), e.getMessage()));	
		}
	}

	/**
	 * Scrittura sull'audit di MSA di un messaggio email parsato sul quale sono stati riscontrati errori
	 * @param mailboxConfiguration
	 * @param parsedMessage
	 * @param exception
	 * @throws Exception
	 */
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception exception) throws Exception {
		try {
			logger.info(String.format(WRITE_AUDIT_MESSAGE_LOG_MESSAGE, mailboxConfiguration.getName(), parsedMessage.getMessageId(), "ERROR", level));
			auditWriter.writeErrorAuditMessage(mailboxConfiguration, parsedMessage, exception);
		}
		catch (Exception e) {
			logger.error(String.format(WRITE_AUDIT_MESSAGE_LOG_ERROR_MESSAGE, parsedMessage.getMessageId(), "error"), e);
			Services.getNotificationService().notifyError(String.format(WRITE_AUDIT_MESSAGE_MAIL_ERROR_MESSAGE, parsedMessage.getMessageId(), e.getMessage()));			
		}
	}
	
	/**
	 * Scrittura sull'audit di MSA di un messaggio email sul quale risulta fallito anche il parsing iniziale
	 * @param mailboxConfiguration
	 * @param message
	 * @param exception
	 * @throws Exception
	 */
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, Message message, Exception exception) throws Exception {
		String messageId = null;
		try {
			messageId = MessageUtils.getMessageId(message);
			logger.info(String.format(WRITE_AUDIT_MESSAGE_LOG_MESSAGE, mailboxConfiguration.getName(), messageId, "ERROR", level));
			auditWriter.writeErrorAuditMessage(mailboxConfiguration, message, exception);
		}
		catch (Exception e) {
			logger.error(String.format(WRITE_AUDIT_MESSAGE_LOG_ERROR_MESSAGE, messageId, "error"), e);
			Services.getNotificationService().notifyError(String.format(WRITE_AUDIT_MESSAGE_MAIL_ERROR_MESSAGE, messageId, e.getMessage()));			
		}
	}
	
	public boolean auditMessageInErrorFound(MailboxConfiguration mailboxConfiguration, String messageId) throws Exception {
		return auditWriter.isErrorMessageFoundInAudit(mailboxConfiguration, messageId);
	}

	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) {
		writeAuditMailboxRun(auditMailboxRun, true);
	}
	
	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun, boolean notifyError) {
		try {
			logger.info(String.format(WRITE_AUDIT_MAILBOX_RUN_LOG_MESSAGE, auditMailboxRun.getMailboxName(), auditMailboxRun.getStatus()));
			auditWriter.writeAuditMailboxRun(auditMailboxRun);
		}
		catch (Exception e) {
			logger.error(String.format(WRITE_AUDIT_MAILBOX_RUN_LOG_ERROR_MESSAGE, auditMailboxRun.getMailboxName()), e);
			if (notifyError)
				Services.getNotificationService().notifyError(String.format(WRITE_AUDIT_MAILBOX_RUN_MAIL_ERROR_MESSAGE, auditMailboxRun.getMailboxName(), e.getMessage()));	
		}
	}
	
}
