package it.tredi.msa;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Classe di avvio del servizio MSA
 */
@SpringBootApplication
public class MsaLauncher implements CommandLineRunner {
	
	private static final Logger logger = LogManager.getLogger(MsaLauncher.class.getName());
	private final static String DEFAULT_MSA_PORT = "8381";

	private static Msa msa;

	@Autowired
	private Environment env;
	
	// Necessario per evitare errori di scrittura su mongodb in fase di shutdown dell'applicazione. Visto che i bean non vengono istanziati
	// tramite Spring, non pare ci sia modo di chiudere i thread di elaborazione delle caselle prima della connessione a MongoDB (Spring non sa
	// che ci sono dipendenze fra i due). Con l'autowired in questa classe viene rimandato alla fine il destroy di mongodb.
	// FIXME Andrebbe fatto un refactor dell'applicazione e istanziati tutti i bean utilizzando String (@Autowired)
	@Autowired
	private MongoTemplate mongoTemplate;  
	
	public static void main(String []args) {
		int exitCode = 0;
		try {
			SpringApplication.run(MsaLauncher.class, args);
		}
		catch (Exception e) {
			logger.fatal(e);
			exitCode = 1;
		}
		System.exit(exitCode);		
	}
	
	protected void logSplashMessage() {
		if (logger.isInfoEnabled()) {
			logger.info("            ..--\"\"|");
			logger.info("            |     |");
			logger.info("            | .---'");
			logger.info("      (\\-.--| |---------.");
			logger.info("     / \\) \\ | |          \\");
			logger.info("     |:.  | | |           |");
			logger.info("     |:.  | |o|           |");
			logger.info("     |:.  | `\"`           |");
			logger.info("     |:.  |_ __  __ _  __ /");
			logger.info("     `\"\"\"\"`\"\"|=`|\"\"\"\"\"\"\"`");
			logger.info("             |=_|");
			logger.info("             |= |");		
			logger.info("  __  __   ____       _    ");
			logger.info(" |  \\/  | / ___|     / \\");   
			logger.info(" | |\\/| | \\___ \\    / _ \\");  
			logger.info(" | |  | |  ___) |  / ___ \\"); 
			logger.info(" |_|  |_| |____/  /_/   \\_\\");
			logger.info("MSA version: " + env.getProperty("application.version") + " " + env.getProperty("build.date"));
		}
	}

	@Override
	public void run(String... args) throws Exception {
		logSplashMessage();
			
		msa = new Msa();
		msa.run(Integer.parseInt(env.getProperty("msa.port", DEFAULT_MSA_PORT)));
	}

	/**
	 * Metodo chiamato per arrestare il servizio
	 */
	public static void stop(String[] args) {
		System.exit(0);
	}
	
	/**
	 * Chiusura dell'applicazione
	 */
	@PreDestroy
	public void onExit() {
		logger.info("onExit(): exit method now call System.exit(0)");
		if (msa != null)
			msa.shutdown();
		
		if (mongoTemplate != null)
			logger.info("MongoDB.dbName = " + mongoTemplate.getDb().getName());
		
		logger.info("onExit(): STOP FROM THE LIFECYCLE!");
		
		//call log4j2 shutdown manullay (see log4l2.xml -> shutdownHook="disable")
		LoggerContext context = (LoggerContext)LogManager.getContext(false);
		context.stop();
	}

}
