package it.tredi.msa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.utils.maven.ApplicationProperties;

public class Msa {
	
	private static final Logger logger = LogManager.getLogger(Msa.class.getName());
	
	private static final String MSA_ARTIFACTID = "msa";
	private static final String MSA_GROUPID = "it.tredi";	
	
	public static void main(String []args) {
		
		splashMessage();
		
	}
	
	private static void splashMessage() {
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
			logger.info("MSA version: " + ApplicationProperties.getInstance().getVersion(MSA_GROUPID, MSA_ARTIFACTID) + " " + ApplicationProperties.getInstance().getBuildDate(MSA_GROUPID, MSA_ARTIFACTID));
		}
	}

}
