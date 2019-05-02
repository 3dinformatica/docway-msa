package it.tredi.msa;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Classe di definizio e avvio di tutti i job di gestione delle caselle di posta (istanziata da classe Launcher). Si occupa
 * del caricamento delle configurazioni di MSA e dell'avvio di tutti i thread paralleli di gestione delle caselle (definizione del pool di thread) 
 */
public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	
	private ExecutorServiceHandler executorServiceHandler;
	
	protected void run(int port) throws Exception {
		// mbernardini 17/12/2018 : corretto controllo su porta socket occupata
		try {
			MsaSocket.getInstance(port);
		}
		catch (Exception e) {
			if (logger.isInfoEnabled()) {
				logger.info("Another instance of MSA already running on port [" + port + "]");
				logger.info("MSA Service NOT Started!");
			}
			throw e;
		}
	
		if (logger.isInfoEnabled())
			logger.info("MSA Service Started on port [" + port + "]");

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
	
	protected void shutdown() {
		if (logger.isInfoEnabled())
			logger.info("shutdown() called");

		executorServiceHandler.shutdown();

		if (logger.isInfoEnabled())
			logger.info("shutdown() successfully completed");
	}

}
