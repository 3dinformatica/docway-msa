package it.tredi.msa;

import java.security.AccessControlException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	private MsaShutdownHook shutdownHook;
	
	private ExecutorServiceHandler executorServiceHandler;
	
	protected void run() throws Exception {
		registerShutdownHook();
	
		if (logger.isInfoEnabled())
			logger.info("MSA Service Started!");

		//load msa configuration and init all services
		Services.init();

		//start exexutor
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(Services.getConfigurationService().getMSAConfiguration().getMailboxManagersPoolsize());
		if (logger.isInfoEnabled())
			logger.info("Executor Service created. Pool size: " + Services.getConfigurationService().getMSAConfiguration().getMailboxManagersPoolsize());
		
		executorServiceHandler = new ExecutorServiceHandler(executor);
		executor.scheduleWithFixedDelay(executorServiceHandler, 0, Services.getConfigurationService().getMSAConfiguration().getMailboxManagersRefreshTime(), TimeUnit.SECONDS);

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        executor.shutdown();
	}
	
	protected void registerShutdownHook() {
		this.shutdownHook = new MsaShutdownHook();
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook);
			if (logger.isInfoEnabled())
				logger.info("MsaShutdownHook registered!");
		} 
		catch (AccessControlException e) {
			logger.error("Could not register shutdown hook " + e.getMessage(), e);
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
				logger.info("MsaShutdownHook hook ACTIVATED. Shutting down");
			try {
				shutdown();
			} 
			catch (Exception e) {
				logger.error("MsaShutdownHook got exception on MSA closure " + e.getMessage(), e);
			}
		}
	}		
	
	private void shutdown() {
		if (logger.isInfoEnabled())
			logger.info("shutdown() called");

		executorServiceHandler.shutdown();

		if (logger.isInfoEnabled())
			logger.info("shutdown() successfully completed");
		
		//call log4j2 shutdown manullay (see log4l2.xml -> shutdownHook="disable")
		LoggerContext context = (LoggerContext)LogManager.getContext(false);
		context.stop();
	}

}
