package it.tredi.msa;

import java.security.AccessControlException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.msa.entity.MailboxConfiguration;


public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	private MsaShutdownHook shutdownHook;
	
	ScheduledExecutorService executor;
	Task []tasks = new Task[3];
	Task task1, task2, task3;
	
	protected void run() throws Exception {
		registerShutdownHook();
		
		try {
			if (logger.isInfoEnabled())
				logger.info("MSA Service Started!");

			//load msa configuration and init all services
			Services.init();

			//load mailbox configurations (via MailboxConfigurationReader(s))
			MailboxConfiguration []mailboxconfigurations = Services.getConfigurationService().readMailboxConfigurations();
			
			//start executor
			executor = Executors.newScheduledThreadPool(2);
	        task1 = tasks[0] = new Task ("Task 1");
	        task2 = tasks[1] = new Task ("Task 2");
	        task3 = tasks[2] = new Task ("Task 3");

	        logger.info("The time is : " + new Date());
	         
	        executor.scheduleWithFixedDelay(task1, 0, 5, TimeUnit.SECONDS);
	        
	        executor.scheduleWithFixedDelay(task2, 0, 7, TimeUnit.SECONDS);
	        executor.scheduleWithFixedDelay(task3, 0, 5, TimeUnit.SECONDS);			
	       //end executor
	        
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
		
		for (int i=0; i<tasks.length; i++)
			tasks[i].shutdown();
		
		logger.info("shutdown() successfully completed");
	}

}
