package it.tredi.msa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

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
		logger.info("Msa.stop(): exit method now call System.exit(0)");
		if (msa != null)
			msa.shutdown();
		System.exit(0);
	}


}
