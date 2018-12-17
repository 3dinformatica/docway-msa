package it.tredi.msa.test;

import java.io.File;
import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;

/**
 * Lettura di file EML da directory resources di test
 */
public class EmlReader {

	protected static final String EML_LOCATION = "eml";
	
	/**
	 * Creazione dell'oggetto message a partire da un file salvato su disco
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	protected Message readEmlFile(File file) throws MessagingException, IOException {
		Message message = null;
		if (file != null && file.exists())
	        message = new MimeMessage(null, FileUtils.openInputStream(file));
		return message;
	}
	
}
