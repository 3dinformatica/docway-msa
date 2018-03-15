package it.tredi.msa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MailboxManagerFactory;

public class ExecutorServiceHandler implements Runnable {

	private boolean shutdown = false;
	private static final Logger logger = LogManager.getLogger(ExecutorServiceHandler.class.getName());
	private ScheduledExecutorService executor;
	
	private Map<String, MailboxManager> mailboxManagersMap;
	
    public ExecutorServiceHandler(ScheduledExecutorService executor) {
		this.executor = executor;
		mailboxManagersMap = new HashMap<String, MailboxManager>();
	}
    
	@Override
    public void run() {
		if (logger.isInfoEnabled())
			logger.info("Refreshing mailbox managers. Loading mailbox configurations...");
		
    	if (!shutdown) {
    		try {
    			//load mailbox configurations (via MailboxConfigurationReader(s))
    			Set<String>	freshMailboxConfigurationsSet = new HashSet<String>();
    			MailboxConfiguration []freshMailboxConfigurations = Services.getConfigurationService().readMailboxConfigurations();
    			
    			//create and start mailbox managers for new configurations
    			int i = 0;
    			for (MailboxConfiguration mailboxConfiguration:freshMailboxConfigurations) {
    				if (!shutdown) {
    					freshMailboxConfigurationsSet.add(mailboxConfiguration.getName());
    					
    					if (mailboxManagersMap.get(mailboxConfiguration.getName()) == null) {

    						if (logger.isInfoEnabled())
    							logger.info("Found new mailbox configuration: " + mailboxConfiguration.getName());
        					
        					MailboxManager mailboxManager = MailboxManagerFactory.createMailboxManager(mailboxConfiguration);
        					mailboxManagersMap.put(mailboxConfiguration.getName(), mailboxManager);
        					
            				int delay = mailboxManager.getConfiguration().getDelay() == -1? Services.getConfigurationService().getMSAConfiguration().getMailboxManagersDelay() : mailboxManager.getConfiguration().getDelay();
            				executor.scheduleWithFixedDelay(mailboxManager, i++, delay, TimeUnit.SECONDS);
        				}
    				}
    			}
    			
    			//stop mailbox managers for deleted configurations
    			for (String confName:mailboxManagersMap.keySet()) {
    				if (!shutdown) {		
    					if (!freshMailboxConfigurationsSet.contains(confName)) {
    						if (logger.isInfoEnabled())
    							logger.info("Found missing(deleted) mailbox configuration: " + confName);
        					
        					mailboxManagersMap.get(confName).shutdown();
        					mailboxManagersMap.remove(confName);
        				}
    				}
    			}
    			
    			if (logger.isInfoEnabled())
    				logger.info("Current mailbox managers: " + keySetToString(mailboxManagersMap.keySet()));
    		}
    		catch (Exception e) {
    			logger.error("Unexpected error. Check mailbox configurations!", e);
    			Services.getNotificationService().notifyError("Errore imprevisto in fase di caricamento delle configurazioni delle caselle di posta.\nConsultare il log per maggiori dettagli.\n\n" + e.getMessage());
    		}   	
    	}
    }
	
    public void shutdown() {
    	shutdown = true;
    	
		if (logger.isInfoEnabled())
			logger.info("Shutting down mailbox managers: " + keySetToString(mailboxManagersMap.keySet()));
    	
    	for (String confName:mailboxManagersMap.keySet())
    		mailboxManagersMap.get(confName).shutdown();
    	
		Thread.currentThread().interrupt();
    }
    
    private String keySetToString(Set<String> set) {
    	String s = "";
    	for (String key:set)
    		s += ", " + key; 	
    	return s.substring(2);
    }
 
}
