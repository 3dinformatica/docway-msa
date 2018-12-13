package it.tredi.msa.mailboxmanager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.mail.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailReader;
import it.tredi.msa.Services;
import it.tredi.msa.audit.AuditMailboxRun;
import it.tredi.msa.audit.AuditMailboxRunStatus;
import it.tredi.msa.configuration.MailboxConfiguration;

public abstract class MailboxManager implements Runnable {
	
	private MailboxConfiguration configuration;
	private MailReader mailReader;
	private boolean shutdown = false;
	private static final Logger logger = LogManager.getLogger(MailboxManager.class.getName());
	private AuditMailboxRun auditMailboxRun;
	private boolean running;
	
	private final static int MAILREADER_CONNECTION_ATTEMPTS = 3;
	private final static String PROCESS_MAILBOX_ERROR_MESSAGE = "Errore imprevisto durante le gestione della casella di posta [%s].\nControllare la configurazione [%s://%s:%s][User:%s]\nConsultare il log per maggiori dettagli.\n\n%s";
	private final static String STORE_MESSAGE_ERROR_MESSAGE = "Errore imprevisto durante l'archiviazione del messaggio di posta [%s].\nMessage Id: %s\nSent Date: %s\nSubject: %s\nConsultare il log per maggiori dettagli.\n\n%s";
	private final static String HANDLE_ERROR_ERROR_MESSAGE = "Errore imprevisto durante la gestione di un errore in fase di archiviazione di un messaggio di posta\nConsultare il log per maggiori dettagli.\n\n%s";
	
	public MailboxConfiguration getConfiguration() {
		return configuration;
	}
	
	public void setConfiguration(MailboxConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public MailReader getMailReader() {
		return mailReader;
	}
	
	public void setMailReader(MailReader mailReader) {
		this.mailReader = mailReader;
	}
	
	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void init() {
		//do nothing - override this one for mailbox manager inizialization after creation
	}
		
	@Override
    public void run() {
		running = true;
    	try {
        	if (!shutdown) {
        		if (logger.isInfoEnabled()) {
        			String pecLog = configuration.isPec()? " [PEC]" : "";
        			logger.info("[" + configuration.getName() + "] starting execution" + " [" + configuration.getUser() + "]" + pecLog);
        		}

            	//TEMPLATE STEP - processMailbox
            	processMailbox(); //customization is achieved via template pattern
            	
            	if (logger.isInfoEnabled()) {
            		logger.info("[" + configuration.getName() + "] execution completed");
            		logger.info("[" + configuration.getName() + "] next execution in (" + configuration.getDelay() + ") s");
            	}
        	}    		
    	}
       	catch (Throwable t) {
    		logger.fatal("[" + configuration.getName() + "] execution failed: " + t);
    		shutdown();
    	}  
    	finally {
    		running = false;
    	}
    }
    
    public void shutdown() {
    	try {
        	shutdown = true;
        	
        	if (logger.isInfoEnabled())
        		logger.info("[" + configuration.getName() + "] shutting down");
        	
        	closeSession();
    		
        	if (logger.isInfoEnabled())
        		logger.info("[" + configuration.getName() + "] shutdown completed");
    	}
    	catch (Exception e) {
    		logger.warn("[" + configuration.getName() + "] shutdown failed: ", e);
    	}
    	finally {
    		Thread.currentThread().interrupt();
    	}
    }	
    
    public void processMailbox() {
    	try {
        	if (logger.isDebugEnabled())
        		logger.debug("[" + configuration.getName() + "] processMailbox() called");	
    		
        	//connection attempts
        	Message[] messages = null;
        	for (int attemptIndex = 1; attemptIndex <= MAILREADER_CONNECTION_ATTEMPTS; attemptIndex++) {
            	try {
            		//TEMPLATE STEP - openSession
            		openSession();	
            		
            		//loop messages
            		if (shutdown)
            			return;
                	messages = mailReader.getMessages();
            		break;
            	}
            	catch (Exception e) {
            		if (logger.isDebugEnabled())
            			logger.debug("[" + configuration.getName() + "] connection failed: (" + attemptIndex + "/" +MAILREADER_CONNECTION_ATTEMPTS + ") attempt. Trying again (1) sec.");
            		if (attemptIndex == MAILREADER_CONNECTION_ATTEMPTS)
            			throw e;
            		Thread.sleep(1000); //1 sec delay
            	}    		
        	}        	
        	
        	if (logger.isInfoEnabled())
        		logger.info("[" + configuration.getName() + "] found (" + messages.length + ") messages");
        	auditMailboxRun.setMessageCount(messages.length);
        	
        	int i=1;
        	for (Message message:messages) { //for each email message
        		if (shutdown)
        			return;
        		
        		ParsedMessage parsedMessage = null;
        		try {
            		//TEMPLATE STEP - parsedMessage
            		parsedMessage = parseMessage(message);
        			
            		if (logger.isInfoEnabled())
            			logger.info("[" + configuration.getName() + "] message (" + (i++) + "/" + messages.length + ") [" + parsedMessage.getMessageId() + "]");
            		
            		if (Services.getAuditService().auditMessageInErrorFound(configuration, parsedMessage)) { //message found in error in audit
            			auditMailboxRun.incrementErrorCount();
            			logger.info("[" + configuration.getName() + "] message skipped [" + parsedMessage.getMessageId() + "]. Message already found in audit in [ERROR] state");
            			continue; //skip to next message
            		}
            		
            		//TEMPLATE STEP - processMessage
        			processMessage(parsedMessage);

        		}
        		catch (Exception e) {
        			//TEMPLATE STEP - handleError
        			handleError(e, parsedMessage==null? message : parsedMessage);
        		}
        	}
    	}
    	catch (Throwable t) {
    		//TEMPLATE STEP - handleError
    		handleError(t, null);
    	}
    	finally {
    		
			//TEMPLATE STEP - closeMessage
			closeSession();	
			
        	if (logger.isDebugEnabled())
        		logger.debug("[" + configuration.getName() + "] processMailbox() done");
    	}
    }
    
    public void openSession() throws Exception {
    	if (logger.isDebugEnabled())
    		logger.debug("[" + configuration.getName() + "] opening mailReader connection");
    	
    	//auditi - init mailbox run obj
    	auditMailboxRun = new AuditMailboxRun();
    	auditMailboxRun.setMailboxName(configuration.getName());
    	auditMailboxRun.setMailboxAddress(configuration.getUser());
    	auditMailboxRun.setStartDate(new Date());
    	auditMailboxRun.setStatus(AuditMailboxRunStatus.SUCCESS);
    	
		mailReader.connect();
		mailReader.openFolder(configuration.getFolderName());
		
		if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.MOVE_TO_FOLDER)
    		mailReader.createFolder(configuration.getStoredMessageFolderName()); //if folder exists this method has no effect
		
    	if (logger.isDebugEnabled())
    		logger.debug("[" + configuration.getName() + "] mailReader connection opened");		
    }

    public void closeSession() {
    	//audit - mailbox run
    	if (auditMailboxRun != null) { //call it just one time
        	auditMailboxRun.setEndDate(new Date());
    		Services.getAuditService().writeAuditMailboxRun(auditMailboxRun, !shutdown);
    		auditMailboxRun = null;    		
    	}
    	
		try {
			if (mailReader != null) {
	        	if (logger.isDebugEnabled())
	        		logger.debug("[" + configuration.getName() + "] closing mailReader connection");				

	        	mailReader.closeFolder();
				mailReader.disconnect();
				
	        	if (logger.isDebugEnabled())
	        		logger.debug("[" + configuration.getName() + "] mailReader connection closed");
			}			
		}
		catch (Exception e) {
			logger.warn("[" + configuration.getName() + "] failed to close mailReader session", e);
		}
    }    
    
    public ParsedMessage parseMessage(Message message) throws Exception {
    	return new ParsedMessage(message);
    }
    
    public void processMessage(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isDebugEnabled())
    		logger.debug("[" + configuration.getName() + "] processMessage() called");
    	
		if (logger.isInfoEnabled()) {
			String pecLog = "";
			if (configuration.isPec())
				pecLog = parsedMessage.isPecMessage()? " [PEC Message]" : parsedMessage.isPecReceipt()? " [PEC Receipt]" : "";
			logger.info("[" + configuration.getName() + "] processing message [" + parsedMessage.getMessageId() + "] [Sent: " + parsedMessage.getSentDate() + "] [Subject: " + parsedMessage.getSubject() + "]" + pecLog);
		}
    	
    	//TEMPLATE STEP - isMessageStorable
    	if (isMessageStorable(parsedMessage)) {
    		//TEMPLATE STEP - storeMessage
    		storeMessage(parsedMessage);
    		
    		//TEMPLATE STEP - messageStored
    		messageStored(parsedMessage);
    	}
    	else {
    		//TEMPLATE STEP - skipMessage
    		skipMessage(parsedMessage);
    	}
    	
    	if (logger.isDebugEnabled())
    		logger.debug("[" + configuration.getName() + "] processMessage() done");
    }

    public void handleError(Throwable t, Object obj) {
    	if (shutdown)
    		logger.warn("[" + configuration.getName() + "] exception during shutdown... ignoring error", t);
    	else {
    		if (obj != null)  { //message exception [parseMessage(), storeMessage]

    			auditMailboxRun.incrementErrorCount();
    			auditMailboxRun.incrementNewErrorCount();
    			
    			try {
            		if (obj instanceof ParsedMessage) { //message exception - processMessage
            			ParsedMessage parsedMessage = (ParsedMessage)obj;
            			
        				//audit - error
        				Services.getAuditService().writeErrorAuditMessage(configuration, parsedMessage, (Exception)t);
            			
            			logger.error("[" + configuration.getName() + "] unexpected error processing message [" + parsedMessage.getMessageId() + "]", t);
            			Services.getNotificationService().notifyError(String.format(STORE_MESSAGE_ERROR_MESSAGE, configuration.getName(), parsedMessage.getMessageId(), parsedMessage.getSentDate(), parsedMessage.getSubject(), t.getMessage()));
            		}
            		else if (obj instanceof Message) { //message exception - parseMessage
            			Message message = (Message)obj;
            			logger.error("[" + configuration.getName() + "] unexpected error parsing message [Sent: " + message.getSentDate() + "] [Subject: " + message.getSentDate() + "]");
            			Services.getNotificationService().notifyError(String.format(STORE_MESSAGE_ERROR_MESSAGE, configuration.getName(), "", message.getSentDate(), message.getSubject(), t.getMessage()));
            		}        				
    			}
    			catch (Exception e) {
    				logger.error("[" + configuration.getName() + "] unexpected error handling message error", e);
    				Services.getNotificationService().notifyError(String.format(HANDLE_ERROR_ERROR_MESSAGE, e.getMessage()));
    			}
    			
    		}
    		else { //error [processMailbox()]
    			logger.error("[" + configuration.getName() + "] [" + configuration.getProtocol() + "://" + configuration.getHost() + ":" + configuration.getPort()  + "][User:" + configuration.getUser()  + "]");
    			logger.error("[" + configuration.getName() + "] unexpected error processing mailbox. Check configuration!", t);
    			Services.getNotificationService().notifyError(String.format(PROCESS_MAILBOX_ERROR_MESSAGE, configuration.getName(), configuration.getProtocol(), configuration.getHost(), configuration.getPort(), configuration.getUser(), t.getMessage()));

    			//stack trace to string
    			StringWriter sw = new StringWriter();
    			PrintWriter pw = new PrintWriter(sw);
    			t.printStackTrace(pw);
    			String sStackTrace = sw.toString();
    			
    			auditMailboxRun.setErrorStackTrace(sStackTrace);
    			auditMailboxRun.setStatus(AuditMailboxRunStatus.ERROR);
    			auditMailboxRun.setErrorMessge(t.getMessage());
    		}
    	}
    }
    
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] storing message [" + parsedMessage.getMessageId() + "]");
    }
    
    public void skipMessage(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] message skipped [" + parsedMessage.getMessageId() + "]");
    }
    
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
    }
    
    public void messageStored(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] message stored [" + parsedMessage.getMessageId() + "]");
    	
    	if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.DELETE_FROM_FOLDER) { //rimozione email
    		if (logger.isInfoEnabled())
    			logger.info("[" + configuration.getName() + "] deleting message [" + parsedMessage.getMessageId() + "]");

    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    	else if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.MOVE_TO_FOLDER) { //spostamento email
    		if (logger.isInfoEnabled())
    			logger.info("[" + configuration.getName() + "] moving message to folder(" + configuration.getStoredMessageFolderName() + ") [" + parsedMessage.getMessageId() + "]");
    		
    		mailReader.copyMessageToFolder(parsedMessage.getMessage(), configuration.getStoredMessageFolderName());
    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    	
    	//audit - success
    	Services.getAuditService().writeSuccessAuditMessage(configuration, parsedMessage);
    	auditMailboxRun.incrementStoredCount();
    }
 
}
