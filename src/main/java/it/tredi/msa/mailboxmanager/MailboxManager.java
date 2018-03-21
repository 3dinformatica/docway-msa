package it.tredi.msa.mailboxmanager;

import javax.mail.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailReader;
import it.tredi.msa.Services;
import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;
import it.tredi.msa.entity.StoredMessagePolicy;

public abstract class MailboxManager implements Runnable {

	private MailboxConfiguration configuration;
	private MailReader mailReader;
	private boolean shutdown = false;
	private static final Logger logger = LogManager.getLogger(MailboxManager.class.getName());
	
	private final static String PROCESS_MAILBOX_ERROR_MESSAGE = "Errore imprevisto durante le gestione della casella di posta [%s].\nControllare la configurazione [%s://%s:%s][User:%s]\nConsultare il log per maggiori dettagli.\n\n%s";
	
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
		
	@Override
    public void run() {
    	try {
        	if (!shutdown) {
        		if (logger.isInfoEnabled())
        			logger.info("[" + configuration.getName() + "] starting execution" + " [" + configuration.getUser() + "]");

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
    		
    		//TEMPLATE STEP - openSession
    		openSession();	
    		
    		//loop messages
    		if (shutdown)
    			return;
        	Message []messages = mailReader.getMessages();
        	
        	if (logger.isInfoEnabled())
        		logger.info("[" + configuration.getName() + "] found (" + messages.length + ") messages");
        	
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
    	
		mailReader.connect();
		mailReader.openInboxFolder();
		
    	if (logger.isDebugEnabled())
    		logger.debug("[" + configuration.getName() + "] mailReader connection opened");		
    }

    public void closeSession() {
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
    	
		if (logger.isInfoEnabled())
			logger.info("[" + configuration.getName() + "] processing message [" + parsedMessage.getMessageId() + "] [Sent: " + parsedMessage.getSentDate() + "] [Subject: " + parsedMessage.getSubject() + "]");
    	
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
//TODO - gestione errore
    		}
    		else { //error [processMailbox()]
    			logger.error("[" + configuration.getName() + "] [" + configuration.getProtocol() + "://" + configuration.getHost() + ":" + configuration.getPort()  + "][User:" + configuration.getUser()  + "]");
    			logger.error("[" + configuration.getName() + "] unexpected error processing mailbox. Check configuration!", t);
    			Services.getNotificationService().notifyError(String.format(PROCESS_MAILBOX_ERROR_MESSAGE, configuration.getName(), configuration.getProtocol(), configuration.getHost(), configuration.getPort(), configuration.getUser(), t.getMessage()));
    		}
    		
    		//se obj == null -> errore non di messaggio -> notificare l'errore tramite mail indicando errore imprevisto nell'archiviazione della casella (indicare il nome)
    		//se obj instance of Message -> errore di parsing (indicare dati minimali) -> indicare errore solo una volta
    		//se obj instance of ParsedMessage -> errore di gestione/archiviazione messaggio -> indicare errore solo una volta
    	
    		//utilizzare l'AUDIT (impostando l'errore)...oppure l'audit va gestito comunque e fare report
    	}
    	
    	//TODO - log error    		
    	//TODO - notificare l'errore con il NOTIFICATION SERVICE (INSERIRE QUA LA LOGICA SE NOTIFICARE O MENO L'ERRORE UTILIZZANDO L'AUDIT (se message != null))
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
    		
    		mailReader.createFolder(configuration.getStoredMessageFolderName()); //if folder exists this method has no effect
    		mailReader.copyMessageToFolder(parsedMessage.getMessage(), configuration.getStoredMessageFolderName());
    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    }
 
}

/*
migliorare il codice in maniera da poter gestire tramite audit per esempio il fatto che un messaggio è stato archiviato ma non cancellato o spostato.
As esempio per farlo si può creare una eccezione per il messageStored ed utilizzarla nell'handleError
*/
