package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.mailboxmanager.ParsedMessage;

/**
 * UnitTest su estrazione contenuto di eml
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EmlExtractionTest.class)
public class EmlExtractionTest {
	
	private static final String EML_LOCATION = "eml";
	
	/**
	 * Creazione dell'oggetto message a partire da un file salvato su disco
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	private Message _readEmlFile(File file) throws MessagingException, IOException {
		Message message = null;
		if (file != null && file.exists())
	        message = new MimeMessage(null, FileUtils.openInputStream(file));
		return message;
	}

	/**
	 * Test di estrazione di un messaggio contenente una parte NULL
	 */
	@Test
	public void partNullExtraction() throws Exception {
		String fileName = "partNull.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(_readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("fabriziobarberini@ordineavvocatiroma.org", fromDatiCert);
	}
	
}
