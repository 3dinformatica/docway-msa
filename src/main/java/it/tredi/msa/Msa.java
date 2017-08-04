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
	
	protected static void splashMessage() {
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


/*
https://javaee.github.io/javamail/


Mailbox - bean contenente la configurazione della casella di posta
Mail - bean con il messaggio di posta

ConfigManager - legge dal file di property dove sono le configurazioni (contiene tutta la lista delle caselle di posta)
ConfigReader - interfaccia (metodo per l'estrazione di caselle di posta)
AclConfigReader - implementa interfaccia ConfigReader 

MailboxManager (interfaccia)
DocWayMailboxManager (salvataggio delle caselle di posta su docway)

ExtraWayService (locator pattern?) con cache dato user e db (renderlo transizionale)

Modulo per la persistenza delle statistiche (ogni metodo che viene chiamato lascia una traccia, ognugno implementa come vuole)


Message









*/