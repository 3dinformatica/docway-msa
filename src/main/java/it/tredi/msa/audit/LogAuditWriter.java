package it.tredi.msa.audit;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.tredi.mail.MessageUtils;
import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.entity.AuditMailboxRun;
import it.tredi.msa.entity.AuditMessageStatus;
import it.tredi.msa.mailboxmanager.ParsedMessage;

/**
 * Implementazione su LOG del writer dell'audit sullo stato di elaborazione di MSA
 */
public class LogAuditWriter extends AuditWriter {
	
	private static final Logger logger = LoggerFactory.getLogger(LogAuditWriter.class);
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public void writeSuccessAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage) throws Exception {
		if (mailboxConfiguration != null && parsedMessage != null) {
			if (logger.isInfoEnabled()) {
				logger.info("[" + mailboxConfiguration.getName() + "]: LOG MESSAGE " +  parsedMessage.getMessageId() + " -> " + AuditMessageStatus.SUCCESS);
				logger.info("\tmailbox.address = " + mailboxConfiguration.getUser());
				logger.info("\tmessage.id = " + parsedMessage.getMessageId());
				logger.info("\tmessage.sendDate = " + dateToAuditLog(parsedMessage.getSentDate()));
				logger.info("\tmessage.subject = " + parsedMessage.getSubject());
				logger.info("\tmessage.from = " + retrieveFromAddress(parsedMessage));
			}
			
			// Eventuale eliminazione del messaggio dalla lista degli errori pendenti
			ErrorMessagesLogAudit.getInstance().removeError(mailboxConfiguration.getName(), parsedMessage.getMessageId());
		}
	}

	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, ParsedMessage parsedMessage, Exception exception) throws Exception {
		if (mailboxConfiguration != null && parsedMessage != null) {
			logger.error("[" + mailboxConfiguration.getName() + "]: LOG MESSAGE " +  parsedMessage.getMessageId() + " -> " + AuditMessageStatus.ERROR);
			logger.error("\tmailbox.address = " + mailboxConfiguration.getUser());
			logger.error("\tmessage.id = " + parsedMessage.getMessageId());
			logger.error("\tmessage.sendDate = " + dateToAuditLog(parsedMessage.getSentDate()));
			logger.error("\tmessage.subject = " + parsedMessage.getSubject());
			logger.error("\tmessage.from = " + retrieveFromAddress(parsedMessage));
			if (exception != null)
				logger.error("\texception = " + exception.getMessage(), exception);
			
			// Scrittura del messageId sulla lista di errori pendenti (mantenuta in memoria)
			ErrorMessagesLogAudit.getInstance().addError(mailboxConfiguration.getName(), parsedMessage.getMessageId());
		}
	}
	
	@Override
	public void writeErrorAuditMessage(MailboxConfiguration mailboxConfiguration, Message message, Exception exception) throws Exception {
		if (mailboxConfiguration != null && message != null) {
			String messageId = MessageUtils.getMessageId(message);
			
			logger.error("[" + mailboxConfiguration.getName() + "]: LOG MESSAGE " +  messageId + " -> " + AuditMessageStatus.ERROR);
			logger.error("\tmailbox.address = " + mailboxConfiguration.getUser());
			logger.error("\tmessage.id = " + messageId);
			logger.error("\tmessage.sendDate = " + dateToAuditLog(message.getSentDate()));
			logger.error("\tmessage.subject = " + message.getSubject());
			logger.error("\tmessage.from = " + MessageUtils.getFromAddress(message));
			if (exception != null)
				logger.error("\texception = " + exception.getMessage(), exception);
			
			// Scrittura del messageId sulla lista di errori pendenti (mantenuta in memoria)
			ErrorMessagesLogAudit.getInstance().addError(mailboxConfiguration.getName(), messageId);
		}
	}
	
	/**
	 * Formattazione di una data per stampa su LOG di audit
	 * @param date
	 * @return
	 */
	private String dateToAuditLog(Date date) {
		String str = null;
		if (date != null)
			str = sdf.format(date);
		return str;
	}

	@Override
	public boolean isErrorMessageFoundInAudit(MailboxConfiguration mailboxConfiguration, String messageId) throws Exception {
		// Controllo se il messageId risulta presente sulla lista di errori pendenti (mantenuta in memoria)
		String mailboxName = (mailboxConfiguration != null) ? mailboxConfiguration.getName() : null;
		return (ErrorMessagesLogAudit.getInstance().containsError(mailboxName, messageId)) ? true : false;
	}

	@Override
	public void writeAuditMailboxRun(AuditMailboxRun auditMailboxRun) throws Exception {
		if (logger.isInfoEnabled() && auditMailboxRun != null) {
			logger.info("[" + auditMailboxRun.getMailboxName() + "]: LOG EXECUTION -> " + auditMailboxRun.getStatus());
			logger.info("\tmailbox.address = " + auditMailboxRun.getMailboxAddress());
			logger.info("\tmailbox.start_date = " + dateToAuditLog(auditMailboxRun.getStartDate()));
			logger.info("\tmailbox.end_date = " + dateToAuditLog(auditMailboxRun.getEndDate()));
			if (auditMailboxRun.getErrorMessage() != null)
				logger.info("\tmailbox.error_message = " + auditMailboxRun.getErrorMessage());
			if (auditMailboxRun.getErrorStackTrace() != null)
				logger.info("\tmailbox.error_stacktrace = " + auditMailboxRun.getErrorStackTrace());
			if (auditMailboxRun.getMessageCount() > 0) { 
				logger.info("\tmailbox.message_count = " + auditMailboxRun.getMessageCount());
				logger.info("\tmailbox.message_store = " + auditMailboxRun.getStoreCount());
				logger.info("\tmailbox.message_skip = " + auditMailboxRun.getSkipCount());
				logger.info("\tmailbox.message_fail = " + auditMailboxRun.getErrorCount());
			}
		}
	}

}
