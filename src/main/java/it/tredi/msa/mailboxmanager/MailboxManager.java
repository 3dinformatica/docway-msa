package it.tredi.msa.mailboxmanager;

import javax.mail.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailReader;
import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.entity.ParsedMessage;
import it.tredi.msa.entity.StoredMessagePolicy;

public abstract class MailboxManager implements Runnable {

	private MailboxConfiguration configuration;
	private MailReader mailReader;
	private boolean shutdown = false;
	private static final Logger logger = LogManager.getLogger(MailboxManager.class.getName());
	
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
    		
    		Thread.currentThread().interrupt();
    	}
    	catch (Exception e) {
    		logger.warn("[" + configuration.getName() + "] shutdown failed: ", e);
    	}
    }	
    
    public void processMailbox() {
    	try {
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
        		
        		//TEMPLATE STEP - parsedMessage
        		ParsedMessage parsedMessage = parseMessage(message);
        		
        		if (logger.isInfoEnabled())
        			logger.info("[" + configuration.getName() + "] processing message (" + (i++) + "/" + messages.length + ") [Sent: " + parsedMessage.getSentDate() + "] [Subject: " + parsedMessage.getSubject() + "]");        		
        		
        		try {
        			//TEMPLATE STEP - processMessage
        			processMessage(parsedMessage);
        		}
        		catch (Exception e) {
        			//TEMPLATE STEP - handleError
        			handleError(e, parsedMessage); 
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
    	}
    }
    
    public void openSession() throws Exception {
		mailReader.connect();
		mailReader.openInboxFolder();
    }

    public void closeSession() {
		try {
			if (mailReader != null) {
				mailReader.closeFolder();
				mailReader.disconnect();
			}
		}
		catch (Exception e) {
			logger.warn("[" + configuration.getName() + "] closeSession() warn", e);
		}
    }    
    
    public ParsedMessage parseMessage(Message message) {
    	return new ParsedMessage(message);
    }
    
    public void processMessage(ParsedMessage parsedMessage) throws Exception {
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
    }

    public void handleError(Throwable t, ParsedMessage parsedMessage) {
    	if (shutdown)
    		logger.warn("[" + configuration.getName() + "] aborted during shutdown", t);
    	else
    		logger.error("[" + configuration.getName() + "] error", t);
    	
    	//if () {
    		// se parsedMessage == null notificare l'errore tramite mail indicando errore imprevisto nell'archiviazione della casella pippo
    	//}
    	
    	//TODO - log error    		
    	//TODO - notificare l'errore con il NOTIFICATION SERVICE (INSERIRE QUA LA LOGICA SE NOTIFICARE O MENO L'ERRORE UTILIZZANDO L'AUDIT (se message != null))
    }
    
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] storing message: " + parsedMessage.getMessageId());
    }
    
    public void skipMessage(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] message skipped: " + parsedMessage.getMessageId());
    }
    
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
    }
    
    public void messageStored(ParsedMessage parsedMessage) throws Exception {
    	if (logger.isInfoEnabled())
    		logger.info("[" + configuration.getName() + "] message stored: " + parsedMessage.getMessageId());
    	
    	if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.DELETE_FROM_FOLDER) { //rimozione email
    		if (logger.isInfoEnabled())
    			logger.info("[" + configuration.getName() + "] deleting message: " + parsedMessage.getMessageId());
    		
    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    	else if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.MOVE_TO_FOLDER) { //spostamento email
    		if (logger.isInfoEnabled())
    			logger.info("[" + configuration.getName() + "] moving message to folder(" + configuration.getStoredMessageFolderName() + "): " + parsedMessage.getMessageId());
    		
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
