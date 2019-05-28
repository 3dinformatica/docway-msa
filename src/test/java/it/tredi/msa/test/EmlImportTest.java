package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 * UnitTest su estrazione del contenuto da eml recuperate da una casella di import (messaggi contenenti in allegato il reale
 * messaggio eml da processare)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class EmlImportTest extends EmlReader {
	
	/**
	 * Percorso alla directory contenente gli esempi di eml di importazione
	 */
	private static final String IMPORT_DIRECTORY = EML_LOCATION + "/import";

	/**
	 * Test di estrazione di un messaggio contenente una parte NULL. EML allegato di 
	 * tipo: com.sun.mail.util.BASE64DecoderStream
	 */
	@Test
	public void emlImportTest01() throws Exception {
		
		// -------------------------------------------------------------------------------------
		// ----- CASO 1
		// -------------------------------------------------------------------------------------
		String fileName = "NoReply01.eml";
		File file = ResourceUtils.getFile("classpath:" + IMPORT_DIRECTORY + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file), true);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		assertEquals("comune.incisa@postacert.toscana.it", parsed.getFromAddress());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("comune.incisa@postacert.toscana.it", fromDatiCert);
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(2, parsed.getAttachments().size());
		
		// -------------------------------------------------------------------------------------
		// ----- CASO 2
		// -------------------------------------------------------------------------------------
		fileName = "NoReply02.eml";
		file = ResourceUtils.getFile("classpath:" + IMPORT_DIRECTORY + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		parsed = new ParsedMessage(readEmlFile(file), true);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		assertEquals("ragioneria.marzanodinola@asmepec.it", parsed.getFromAddress());
		
		fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("ragioneria.marzanodinola@asmepec.it", fromDatiCert);
		
		attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(2, parsed.getAttachments().size());
		
		// -------------------------------------------------------------------------------------
		// ----- CASO 3
		// -------------------------------------------------------------------------------------
		fileName = "NoReply03.eml";
		file = ResourceUtils.getFile("classpath:" + IMPORT_DIRECTORY + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		parsed = new ParsedMessage(readEmlFile(file), true);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		assertEquals("verghereto@pec.unionevallesavio.it", parsed.getFromAddress());
		
		fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("verghereto@pec.unionevallesavio.it", fromDatiCert);
		
		attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(2, parsed.getAttachments().size());
		
		// -------------------------------------------------------------------------------------
		// ----- CASO 4
		// -------------------------------------------------------------------------------------
		fileName = "NoReply04.eml";
		file = ResourceUtils.getFile("classpath:" + IMPORT_DIRECTORY + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		parsed = new ParsedMessage(readEmlFile(file), true);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		assertEquals("cciaa@pec.milomb.camcom.it", parsed.getFromAddress());
		
		fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("cciaa@pec.milomb.camcom.it", fromDatiCert);
		
		attachments = parsed.getAttachmentsName();
		boolean confermaFound = false;
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments) {
			System.out.println("\tattach name = " + name);
			if (name.equals("Conferma.pdf"))
				confermaFound = true;
		}
		
		assertEquals(3, parsed.getAttachments().size());
		assertTrue(confermaFound);
		
		// -------------------------------------------------------------------------------------
		// ----- CASO 5
		// -------------------------------------------------------------------------------------
		fileName = "NoReply05.eml";
		file = ResourceUtils.getFile("classpath:" + IMPORT_DIRECTORY + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		parsed = new ParsedMessage(readEmlFile(file), true);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		assertEquals("protocollo@pec.comune.bovezzo.bs.it", parsed.getFromAddress());
		
		fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("protocollo@pec.comune.bovezzo.bs.it", fromDatiCert);
		
		attachments = parsed.getAttachmentsName();
		boolean segnaturaFound = false;
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments) {
			System.out.println("\tattach name = " + name);
			if (name.equals("SEGNATURA.XML"))
				segnaturaFound = true;
		}
		
		assertEquals(3, parsed.getAttachments().size());
		assertTrue(segnaturaFound);
		
	}
	
}
