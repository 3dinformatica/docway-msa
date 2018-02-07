package it.tredi.msa;

import java.security.AccessControlException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.entity.MailboxConfiguration;
import it.tredi.msa.mailboxmanager.MailboxManager;
import it.tredi.msa.mailboxmanager.MailboxManagerFactory;


public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	private MsaShutdownHook shutdownHook;
	
	private MailboxManager []mailboxManagers;
	
	protected void run() throws Exception {
		registerShutdownHook();
		
		try {
			if (logger.isInfoEnabled())
				logger.info("MSA Service Started!");

			//load msa configuration and init all services
			Services.init();

			//load mailbox configurations (via MailboxConfigurationReader(s))
			MailboxConfiguration []mailboxConfigurations = Services.getConfigurationService().readMailboxConfigurations();
			
			//create mailbox managers
			mailboxManagers = MailboxManagerFactory.createMailboxManagers(mailboxConfigurations);

			//start exexutor
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(Services.getConfigurationService().getMSAConfiguration().getMailboxManagersPoolsize());
			int i = 0;
			for (MailboxManager mailboxManager:mailboxManagers) {
				int delay = mailboxManager.getConfiguration().getDelay() == -1? Services.getConfigurationService().getMSAConfiguration().getMailboxManagersDelay() : mailboxManager.getConfiguration().getDelay();
				executor.scheduleWithFixedDelay(mailboxManager, 5*i++, delay, TimeUnit.SECONDS);
			}
				
	        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	        executor.shutdown();
		}	
		catch (Exception e) {
			logger.error("[FATAL ERROR] -> " + e.getMessage() + "... MSA service is down!", e);
			throw e;
		}
		
	}
	
	protected void registerShutdownHook() {
		this.shutdownHook = new MsaShutdownHook();
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			if (logger.isInfoEnabled())
				logger.info("MsaShutdownHook registered!");
		} 
		catch (AccessControlException e) {
			logger.error("Could not register shutdown hook... " + e.getMessage(), e);
		}		
	}	

	/**
	 * Called on shutdown. This gives use a chance to store the keys and to optimize even if the cache manager's shutdown method was not called
	 * manually.
	 */
	class MsaShutdownHook extends Thread {

		/**
		 * This will persist the keys on shutdown.
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			if (logger.isInfoEnabled())
				logger.info("MsaShutdownHook hook ACTIVATED. Shutting down...");
			try {
				shutdown();
			} 
			catch (Exception e) {
				logger.error("MsaShutdownHook got exception on MSA closure... " + e.getMessage(), e);
			}
		}
	}		
	
	
	private void shutdown() {
		logger.info("shutdown() called");
		
		for (MailboxManager mailboxManager:mailboxManagers)
			mailboxManager.shutdown();
		
		logger.info("shutdown() successfully completed");
	}

}
