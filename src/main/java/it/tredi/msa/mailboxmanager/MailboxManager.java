package it.tredi.msa.mailboxmanager;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailReader;
import it.tredi.msa.entity.MailboxConfiguration;

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
        		try {
        			//TEMPLATE STEP - processMessage
        			processMessage(message);	
        		}
        		catch (Exception e) {
        			//TEMPLATE STEP - handleError
        			handleError(e, message); 
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
    
    public void processMessage(Message message) throws Exception {
    	//TEMPLATE STEP - isMessageStorable
    	if (isMessageStorable(message)) {
    		//TEMPLATE STEP - storeMessage
    		storeMessage(message);
    		
    		//TEMPLATE STEP - messageStored
    		messageStored(message);
    	}
    	else {
    		//TEMPLATE STEP - skipMessage
    		skipMessage(message);
    	}
    }

    public void handleError(Throwable t, Message message) {
    	if (shutdown)
    		logger.warn(configuration.getName() + " - aborted during shutdown", t);
    	else
    		logger.error(configuration.getName() + " - error", t);       	
    	//TODO - log error    		
    	//TODO - notificare l'errore con il NOTIFICATION SERVICE (INSERIRE QUA LA LOGICA SE NOTIFICARE O MENO L'ERRORE UTILIZZANDO L'AUDIT (se message != null))
    }
    
    public void storeMessage(Message message) throws Exception {
    	logger.info("storing message " + message.getSubject());
    }
    
    public void skipMessage(Message message) throws Exception {
    	logger.info("message skipped: " + message.getSubject());
    }
    
    public boolean isMessageStorable(Message message) {
    	return true;
    }
    
    public void messageStored(Message message) throws Exception {
    	//TODO //gestione cancellazione / spostamento
//TODO - COMPLETARE!!!!    	
    	//mailReader.deleteMessage(message);
    	
    }
 
}
