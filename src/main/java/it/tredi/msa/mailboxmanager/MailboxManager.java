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
    	if (!shutdown) {
        	logger.info(configuration.getName() + " - run()");

        	//TEMPLATE STEP - processMailbox
        	processMailbox(); //customization is achieved via template pattern
        	
            logger.info(configuration.getName() + " - done!");        		
    	}
    }
    
    public void shutdown() {
    	shutdown = true;
    	logger.info(configuration.getName() + " - shutting down task");
    	closeSession();
		Thread.currentThread().interrupt();
    }	
    
    public void processMailbox() {
    	try {
    		//TEMPLATE STEP - openSession
    		openSession();	
    		
    		//loop messages
    		if (shutdown)
    			return;
        	Message []messages = mailReader.getMessages();
        	for (Message message:messages) {
        		if (shutdown)
        			return;        		
        		//TEMPLATE STEP - parsedMessage
        		ParsedMessage parsedMessage = parseMessage(message);
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
			logger.warn(configuration.getName() + " - shutdown warn", e);
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
    		logger.warn(configuration.getName() + " - aborted during shutdown", t);
    	else
    		logger.error(configuration.getName() + " - error", t);       	
    	//TODO - log error    		
    	//TODO - notificare l'errore con il NOTIFICATION SERVICE (INSERIRE QUA LA LOGICA SE NOTIFICARE O MENO L'ERRORE UTILIZZANDO L'AUDIT (se message != null))
    }
    
    public void storeMessage(ParsedMessage parsedMessage) throws Exception {
    	logger.info("storing message " + parsedMessage.getSubject());
    }
    
    public void skipMessage(ParsedMessage parsedMessage) throws Exception {
    	logger.info("message skipped: " + parsedMessage.getSubject());
    }
    
    public boolean isMessageStorable(ParsedMessage parsedMessage) {
    	return true;
    }
    
    public void messageStored(ParsedMessage parsedMessage) throws Exception {
    	if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.DELETE_FROM_FOLDER) { //rimozione email
    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    	else if (configuration.getStoredMessagePolicy() == StoredMessagePolicy.MOVE_TO_FOLDER) { //spostamento email
    		mailReader.createFolder(configuration.getStoredMessageFolderName()); //if folder exists this method has no effect
    		mailReader.copyMessageToFolder(parsedMessage.getMessage(), configuration.getStoredMessageFolderName());
    		mailReader.deleteMessage(parsedMessage.getMessage());
    	}
    }
 
}
