package it.tredi.msa;

import java.util.Date;

import javax.mail.Message;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.mail.MailClientHelper;
import it.tredi.mail.MailReader;

class Task implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(Task.class.getName());
	
    private String name;
    private boolean shutdown = false;
 
    public Task(String name) {
        this.name = name;
    }
     
    public String getName() {
        return name;
    }
 
    MailReader mailReader;
    
    @Override
    public void run() 
    {
        try {
        	if (!shutdown) {
            	logger.info(name + " - run()");
                //Thread.sleep(1000);
            	
        		mailReader = MailClientHelper.createMailReader("imap.gmail.com", 993, "mb-proto@3di.it", "!3dinformatica! ", "imaps");
        		mailReader.connect();
        		mailReader.openInboxFolder();
        		Message []messages = mailReader.getMessages();
        		
        		for (int i=0; i<messages.length; i++)
        			logger.info(name + ": " + messages[i].getSubject());
        		
        		mailReader.closeFolder();
        		mailReader.disconnect();        	
        		
            	
                logger.info(name + " - completed");        		
        	}
        }
        catch (Exception e) {
        	if (shutdown)
        		logger.warn(name + " - aborted during shutdown", e);
        	else
        		logger.error(name + " - error", e);
        }
    }
    
    public void shutdown() {
    	shutdown = true;
    	logger.info(name + " - shutting down task");
		try {
			if (mailReader != null) {
				mailReader.closeFolder();
				mailReader.disconnect();
			}
		}
		catch (Exception e) {
			logger.warn(name + " - shutdown warn", e);
		}
		Thread.currentThread().interrupt();
    }

    
}
