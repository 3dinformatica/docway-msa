package it.tredi.msa.mailboxmanager;

import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;

public class MessageParserThred extends Thread {
	
	private static final Logger logger = LogManager.getLogger(MessageParserThred.class.getName());

	private MessageParserThreadWorkObj toDo;
	private CountDownLatch latch;
	
    public MessageParserThred(CountDownLatch latch, MessageParserThreadWorkObj toDo) {
		super();
		this.toDo = toDo;
		this.latch = latch;
	}

	@Override
    public void run() {
    	try {
			if (logger.isInfoEnabled())
				logger.info("[" + toDo.getMailboxAddress() + "] parsing message (" + toDo.getMessageIndex()  + "/" + toDo.getMessageCount() + ")...");
			
			DocwayParsedMessage parsedMessage = new DocwayParsedMessage(toDo.getMessage());
    		toDo.setDONE(parsedMessage);
    		
    		if (logger.isInfoEnabled())
    			logger.info("[" + toDo.getMailboxAddress() + "] message (" + toDo.getMessageIndex() + "/" + toDo.getMessageCount() + ") [" + parsedMessage.getMessageId() + "]");
    		
    		// TODO - vedere se occorre gestire timeout    		
    		    		
    	}
    	catch (Exception e) {
    		logger.error("[" + toDo.getMailboxAddress() + "] unable to parse message (" + toDo.getMessageIndex() + "/" + toDo.getMessageCount() + ")..." + e.getMessage(), e);
    		
    		toDo.setERROR(e);
    	}
    	finally {
    		latch.countDown();
    	}
        	
    }

}
