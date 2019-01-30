package it.tredi.msa.test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
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
		return readEmlFile(file, true);
	}
	
	/**
	 * Creazione dell'oggetto message a partire da un file salvato su disco
	 * @param file
	 * @param mimeAddressStrict
	 * @return
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	protected Message readEmlFile(File file, boolean mimeAddressStrict) throws MessagingException, IOException {
		Message message = null;
		if (file != null && file.exists()) {
			Properties props = new Properties(System.getProperties());
			props.put("mail.mime.address.strict", String.valueOf(mimeAddressStrict));
			//props.put("mail.mime.allowutf8", String.valueOf(mimeAllowutf8));
			
			message = new MimeMessage(Session.getInstance(props), FileUtils.openInputStream(file));
		}
		return message;
	}
	
}
