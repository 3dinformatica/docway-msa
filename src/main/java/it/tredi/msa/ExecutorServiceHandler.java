package it.tredi.msa;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.configuration.MailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MailboxManagerFactory;

public class ExecutorServiceHandler implements Runnable {

	private boolean shutdown = false;
	private static final Logger logger = LogManager.getLogger(ExecutorServiceHandler.class.getName());
	private ScheduledExecutorService executor;
	
	private final static String LOADING_CONFIGURATION_ERROR_MESSAGE = "Errore imprevisto in fase di caricamento delle configurazioni delle caselle di posta.\nConsultare il log per maggiori dettagli.\n\n%s";
	
	public ExecutorServiceHandler(ScheduledExecutorService executor) {
		this.executor = executor;
	}
    
	@Override
    public void run() {
		if (logger.isInfoEnabled())
			logger.info("Refreshing mailbox managers. Loading mailbox configurations");
		
    	if (!shutdown) {
    		try {
    			//load mailbox configurations (via MailboxConfigurationReader(s))
    			Set<String>	freshMailboxConfigurationsSet = new HashSet<String>();
    			MailboxConfiguration[] freshMailboxConfigurations = Services.getConfigurationService().readMailboxConfigurations();
    			
    			//add new configurations and refresh existing ones
    			int i = 0;
    			for (MailboxConfiguration mailboxConfiguration:freshMailboxConfigurations) {
    				if (!shutdown) {
    					freshMailboxConfigurationsSet.add(mailboxConfiguration.getUser());
    					
    					if (MailboxesManagersMap.getInstance().getManager(mailboxConfiguration.getUser()) == null) { //create and start mailbox managers for new configurations

    						if (logger.isDebugEnabled())
    							logger.debug("Found new mailbox configuration: [" + mailboxConfiguration.getUser() + " / " + mailboxConfiguration.getName() + "]");
        					
        					MailboxManager mailboxManager = MailboxManagerFactory.createMailboxManager(mailboxConfiguration);
        					MailboxesManagersMap.getInstance().addManager(mailboxManager);
            				mailboxManager.getConfiguration().setDelay(mailboxManager.getConfiguration().getDelay() == -1? Services.getConfigurationService().getMSAConfiguration().getMailboxManagersDelay() : mailboxManager.getConfiguration().getDelay());
            				executor.scheduleWithFixedDelay(mailboxManager, i++, mailboxManager.getConfiguration().getDelay(), TimeUnit.SECONDS);
        				}
    					else { //update existing configuration
    						if (logger.isDebugEnabled())
    							logger.debug("Updating mailbox configuration: [" + mailboxConfiguration.getUser() + " / " + mailboxConfiguration.getName() + "]");
    						
    						if (!MailboxesManagersMap.getInstance().getManager(mailboxConfiguration.getUser()).isRunning())
    							MailboxManagerFactory.update(MailboxesManagersMap.getInstance().getManager(mailboxConfiguration.getUser()), mailboxConfiguration);
    						else if (logger.isDebugEnabled())
    							logger.debug("Could not update mailbox configuration: [" + mailboxConfiguration.getUser() + " / " + mailboxConfiguration.getName() + "] because mailbox manager is running");
    					}
    					
    				}
    			}
    			
    			//stop mailbox managers for deleted configurations
    			for (String emailAddress:MailboxesManagersMap.getInstance().getMap().keySet()) {
    				if (!shutdown) {
    					if (!freshMailboxConfigurationsSet.contains(emailAddress)) {
    						if (logger.isDebugEnabled())
    							logger.debug("Found missing (deleted) mailbox configuration: [" + emailAddress + "]. Removing it");
        					
    						if (!MailboxesManagersMap.getInstance().getManager(emailAddress).isRunning()) {
    							MailboxesManagersMap.getInstance().getManager(emailAddress).shutdown();
    							MailboxesManagersMap.getInstance().removeManager(emailAddress);    							
    						}
    						else if (logger.isDebugEnabled())
    							logger.debug("Could not remove mailbox configuration: [" + emailAddress + "] because mailbox manager is running");

        				}
    				}
    			}

    		}
    		catch (Throwable t) {
    			logger.error("Unexpected error. Check mailbox configurations!", t);
    			Services.getNotificationService().notifyError(String.format(LOADING_CONFIGURATION_ERROR_MESSAGE, t.getMessage()));
    		}   
    		finally {
    			if (logger.isInfoEnabled()) {
    				logger.info("Current mailbox managers: [" + keySetToString(MailboxesManagersMap.getInstance().getMap().keySet()) + "]");
    				try {
    					logger.info("Next refesh in (" + Services.getConfigurationService().getMSAConfiguration().getMailboxManagersRefreshTime() + ") s");	
    				}
    				catch (Exception logE) {
    					; //ignore it
    				}
    			}    			
    		}
    	}
    }
	
    public void shutdown() {
    	shutdown = true;
    	
		if (logger.isInfoEnabled())
			logger.info("Shutting down mailbox managers: [" + keySetToString(MailboxesManagersMap.getInstance().getMap().keySet()) + "]");
    	
    	for (String emailAddress:MailboxesManagersMap.getInstance().getMap().keySet()) {
    		try {
    			MailboxesManagersMap.getInstance().getManager(emailAddress).shutdown();	
    		}
			catch (Exception e) {
				logger.warn("Shutdown failed: " + emailAddress, e);
			}
    	}
    	
		Thread.currentThread().interrupt();
    }
    
    private String keySetToString(Set<String> set) {
    	String s = "";
    	for (String key:set)
    		s += ", " + key;
    	if (s.length() > 0)
    		s = s.substring(2);
    	return s;
    }
 
}
