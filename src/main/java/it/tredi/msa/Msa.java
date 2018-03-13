package it.tredi.msa;

import java.security.AccessControlException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	private MsaShutdownHook shutdownHook;
	
	private ExecutorServiceHandler executorServiceHandler;
	
	protected void run() throws Exception {
		registerShutdownHook();
		
		try {
			if (logger.isInfoEnabled())
				logger.info("MSA Service Started!");

			//load msa configuration and init all services
			Services.init();

			//start exexutor
			ScheduledExecutorService executor = Executors.newScheduledThreadPool(Services.getConfigurationService().getMSAConfiguration().getMailboxManagersPoolsize());
			executorServiceHandler = new ExecutorServiceHandler(executor);
			executor.scheduleWithFixedDelay(executorServiceHandler, 0, Services.getConfigurationService().getMSAConfiguration().getMailboxManagersRefreshTime(), TimeUnit.SECONDS);

	        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	        executor.shutdown();
		}	
		catch (Exception e) {
			logger.fatal("[FATAL ERROR] -> " + e.getMessage() + "... MSA service is down!", e);
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
		
		executorServiceHandler.shutdown();
		
		logger.info("shutdown() successfully completed");
	}

}
