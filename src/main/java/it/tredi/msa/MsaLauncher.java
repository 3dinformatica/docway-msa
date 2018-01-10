package it.tredi.msa;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.utils.maven.ApplicationProperties;

public class MsaLauncher {
	
	private static final Logger logger = LogManager.getLogger(MsaLauncher.class.getName());
	
	private static final String MSA_ARTIFACTID = "msa";
	private static final String MSA_GROUPID = "it.tredi";	
	
	public static void main(String []args) {
		int exitCode = 0;
		try {
			logSplashMessage();
			
			Msa msa = new Msa();
			msa.run();
		}
		catch (Exception e) {
			logger.error(e);
			exitCode = 1;
		}
		System.exit(exitCode);		
	}
	
	protected static void logSplashMessage() {
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


//VEDERE CLASSE MailStorage.java in progetto Trash di Workspace vecchio Eclipse

/*
https://javaee.github.io/javamail/

ConfigManager - legge dal file di property dove sono le configurazioni (contiene tutta la lista delle caselle di posta)
ConfigReader - interfaccia (metodo per l'estrazione di caselle di posta)
AclConfigReader - implementa interfaccia ConfigReader 

MailboxManager (interfaccia)
DocWayMailboxManager (salvataggio delle caselle di posta su docway)

ExtraWayService (locator pattern?) con cache dato user e db (renderlo transizionale)

Modulo per la persistenza delle statistiche (ogni metodo che viene chiamato lascia una traccia, ognugno implementa come vuole)


Message

***************************************************************************************************************************************

FUNZIONI UTILIZZATE PER LE EMAIL


unpackPEC -> vedi it.tredi.jsf.mail.MailMessage (unpackpec oppure si tratta di un estrattore di info e allegati da un mail message???)

estrazione testo ed HTML dalle parti del messaggio -> vedi it.tredi.jsf.mail.MailMessage

fatturaPA mailAnalyzer -> it.highwaytech.fatturepa.analyzer.MailAnalizer.java

estrazione di un allegato in particolar da un mime message (EML)
    
	private static void extractFile(Multipart multiPart, String fileName) throws Exception {
		for (int i = 0; i < multiPart.getCount(); i++) {
			MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
			if (part.getContentType().contains("multipart")) {
				extractFile((Multipart)part.getContent(), fileName);
			}
			else if (part.getContentType().contains("message/rfc822")) {
				MimeMessage message = (MimeMessage)part.getContent();
				extractFile((Multipart)message.getContent(), fileName);
			}			
			else if (part.getFileName() != null && part.getFileName().equals(fileName))
				retPart = part;
		}
	}    
	
	public String getFromEmail() throws Exception {
        // Federico 01/09/09: riscritto il metodo per individuare l'indirizzo email del mittente in modo affidabile [EB 130]
        String sender = GenericUtils.cleanForQuery(rm.from());
        ArrayList<String> emailAddresses = GenericUtils.extractEmailAddresses(sender);
        return emailAddresses.size() > 0 ? emailAddresses.get(0) : null;
    }	


	public void sendMail(subject, body, sourceAddr, destEmail, sourceName) {
    	
	
		String today = (new java.text.SimpleDateFormat("yyyyMMdd")).format(new java.util.Date());
    	String cod_rpa = document.getAttributeValue("//rif_interni/rif[@diritto='RPA']/@cod_persona");    	
    	
    	String emailDest = "";
    	if (emailRecipient != null && emailRecipient.length() > 0) {
    		emailDest = emailRecipient;
    	}
    	else {
        	//recupero dell'indirizzo email dell'rpa
        	connessione.connect(connessione.getDbStruttur());	
        	try {
        		emailDest = EMail.getEmailWithMatricola(connessione, cod_rpa);
        	}
        	catch (Exception e) {
        		throw e;
        	}
        	finally {
        		connessione.restoreConnect();
        	}
    	}
    	logger.info("...recipient: " + emailDest);
    	
    	//costruzione subject della mail
    	String subject = GenericUtils.parseModel(emailSubject, document);
    	if (subject.length() > EquiNotifier.MAX_SUBJECT_LENGTH)
    		subject = subject.substring(0, EquiNotifier.MAX_SUBJECT_LENGTH) + "...";
    		
    	//costruzione body della mail
        Protocollo protocollo = new Protocollo(document, today, null, null);
        protocollo.setNomeApp(null);
        protocollo.setNomePackage(null);
        protocollo.setConnessione(connessione);
        String doc_body = protocollo.getBodyForEmail(emailWebServerHost, emailWebServerSecondaryHost, emailWebServerUri, theDb);
        
        //invio della mail
        MimeMessage msg = new MimeMessage(ss);

        //sender
        msg.setFrom(new InternetAddress(emailSenderAddr, emailSender, ENCODING));

        //recipient
    	String []addresses = emailDest.split(",");        
        //InternetAddress[] address = { new InternetAddress(emailDest) };
    	InternetAddress[] address = new InternetAddress[addresses.length];
		for (int i=0; i<addresses.length; i++)
			address[i] = new InternetAddress(addresses[i]); 
        msg.setRecipients(Message.RecipientType.TO, address);

        //subject
        msg.setSubject(subject, ENCODING);

        Multipart mp = new MimeMultipart();

        // create and fill the text message part
        MimeBodyPart mbpText = new MimeBodyPart();        
        mbpText.setText(doc_body, ENCODING);
        
        // create the Multipart and its parts to it
        mp.addBodyPart(mbpText);
        
        if (!disableAttachments) {
            // aggiungo gli allegati
            MimeBodyPart attach;
            DataHandler dh;
            byte[] b;
            BinData binData;
            java.net.FileNameMap fnmap = java.net.URLConnection.getFileNameMap();
            List<Element> attachmentL = document.selectNodes("//*[name()='xw:file'][count(./xw:file)=0][count(@der_from)=0]");
            for (int i=0; i< attachmentL.size(); i++) { //per ogni allegato
                Element el = attachmentL.get(i);     
                String id = el.attributeValue("name");
                String extension = id.substring(id.lastIndexOf("."));
                String fileName = el.attributeValue("title") != null? el.attributeValue("title") : id;
                if (!fileName.toUpperCase().endsWith(extension.toUpperCase()))
                	fileName = fileName + extension;
                String mime = fnmap.getContentTypeFor(id);
                if (mime == null) mime = it.highwaytech.util.Mime.ext2ContentType(id);
                
                //estrazione allegato da extraway
                binData = connessione.getBroker().getAttachData(connessione.getConnection(), connessione.getDb(), id.trim());            
                b = binData.content;
                int dataOffset = 0;
                while (b[dataOffset] != 0) dataOffset++;
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
                ba.write(binData.content, ++dataOffset, binData.length);
                byte[] fdata = ba.toByteArray();
                
                //aggiunta allegato
                attach= new MimeBodyPart();
                dh = new DataHandler(new it.highwaytech.util.mailer.BytesDataSource(fdata, mime));
                attach.setDataHandler(dh);
                attach.setFileName(fileName);
                attach.setHeader("Content-Type", mime);
                attach.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");            
                mp.addBodyPart(attach);
            }        	
        }

        // add the Multipart to the message
        msg.setContent(mp);

        // set the Date: header
        msg.setSentDate(new Date());

        // send the message
        msg.saveChanges(); // don't forget this
        tr.sendMessage(msg, msg.getAllRecipients());
	}


*/