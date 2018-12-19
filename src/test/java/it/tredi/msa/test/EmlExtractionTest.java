package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.mailboxmanager.ParsedMessage;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su estrazione contenuto di eml
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class EmlExtractionTest extends EmlReader {
	
	/**
	 * Test di estrazione di un messaggio contenente una parte NULL
	 */
	@Test
	public void partNullExtraction() throws Exception {
		String fileName = "partNull.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("fabriziobarberini@ordineavvocatiroma.org", fromDatiCert);
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(5, parsed.getAttachments().size());
	}
	
	/**
	 * Test di estrazione di un messaggio contenente piu' istanze di daticert.xml (email contenente inoltro di altre email). Deve
	 * essere recuperato e letto il daticert.xml della mail ricevuta
	 * @throws Exception
	 */
	@Test
	public void notWellFormedExtraction() throws Exception {
		String fileName = "notWellFormed.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(13, parsed.getAttachments().size());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("paolapenna@ordineavvocatiroma.org", fromDatiCert);
	}
	
	/**
	 * Errore NotWellFormed su ServerCommand.send su eXtraWay server
	 * @throws Exception
	 */
	@Test
	public void notWellFormed2Extraction() throws Exception {
		String fileName = "notWellFormed_2.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(5, parsed.getAttachments().size());
	}
	
}
