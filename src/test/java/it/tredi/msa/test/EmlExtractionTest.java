package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Ignore;
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
	 * Estrazione dati da messaggio contenente molteplici istanze del file daticert.xml (inoltri vari di email)
	 * @throws Exception
	 */
	@Test
	public void nullPointerMultiDatiCertExtraction() throws Exception {
		String fileName = "nullPointer_multi_daticert.eml";
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
		
		// TODO la mail contiene un allegato messaggio-originale.eml che non viene elaborato (si tratta di un allegato presente in una mail inoltrata)
		assertEquals(11, parsed.getAttachments().size());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("dp.Padova@pce.agenziaentrate.it", fromDatiCert);
	}
	
	/**
	 * Parsing di parti del messaggio incluse come SharedByteArrayInputStream
	 * @throws Exception
	 */
	@Test
	public void classCastExceptionExtraction() throws Exception {
		String fileName = " java.lang.ClassCastException.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		boolean filefound = false;
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments) {
			System.out.println("\tattach name = " + name);
			if (name.equals("Istituto Fiduciario Lombardo SpA.pdf"))
				filefound = true;
		}
		
		assertEquals(3, parsed.getAttachments().size());
		assertTrue(filefound);
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("legal@pec.lacolombofinanziaria.com", fromDatiCert);
	}
	
	/**
	 * Estrazione dati da messaggio contenente molteplici istanze del file daticert.xml (inoltri vari di email)
	 * @throws Exception
	 */
	@Test
	public void nullPointerMultiDatiCert2Extraction() throws Exception {
		String fileName = "nullPointer_multi_daticert_2.eml";
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
		
		assertEquals(15, parsed.getAttachments().size());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("LOM.sospensioni.discarichi@pec.agenziariscossione.gov.it", fromDatiCert);
	}
	
	/**
	 * Estrazione dati da messaggio contenente spazi o caratteri di controllo su un indirizzo 
	 * email: javax.mail.internet.AddressException: Domain contains control or whitespace
	 * @throws Exception
	 */
	@Test
	public void domainContainsControlOrWhitespaceExtraction() throws Exception {
		String fileName = "domain_contains_control_or_whitespace.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file, false));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		System.out.println("to addresses = " + parsed.getToAddressesAsString());
		assertNotNull(parsed.getToAddressesAsString());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("t.c.m.srl@pec.it", fromDatiCert);
	}
	
	/**
	 * Estrazione dati da messaggio in caso di eccezione nel parsing dell'indirizzo 
	 * email: javax.mail.internet.AddressException: Missing '<'
	 * @throws Exception
	 */
	@Test
	public void addressExceptionExtraction() throws Exception {
		String fileName = "address_exception.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file, false));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(2, parsed.getAttachments().size());
		
		System.out.println("to addresses = " + parsed.getToAddressesAsString());
		assertNotNull(parsed.getToAddressesAsString());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("riccardoguerra@pec.ordineavvocatigrosseto.com", fromDatiCert);
	}
	
	/**
	 * Estrazione dati da messaggio in caso di errori di encoding all'interno del file daticert.xml
	 * @throws Exception
	 */
	@Test
	public void daticertEncodingExceptionExtraction() throws Exception {
		/*
		String fileName = null;
		File file = null;
		ParsedMessage parsed = null;
		List<String> attachments = null;
		String fromDatiCert = null;
		*/
		
		// CASO 1
		String fileName = "daticertEncoding.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file, false));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		System.out.println("to addresses = " + parsed.getToAddressesAsString());
		assertNotNull(parsed.getToAddressesAsString());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("consulenza.legale@pec.bppb.it", fromDatiCert);
		
		// CASO 2
		fileName = "daticertEncoding2.eml";
		file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		parsed = new ParsedMessage(readEmlFile(file, false));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		System.out.println("to addresses = " + parsed.getToAddressesAsString());
		assertNotNull(parsed.getToAddressesAsString());
		
		fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("pec@pec.easybc.it", fromDatiCert);
	}
	
	/**
	 * Estrazione dati da messaggio in caso di eccezione di encoding sul 
	 * contenuto: java.io.UnsupportedEncodingException: utf-7
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void utf7ErrorExtraction() throws Exception {
		String fileName = "utf7.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		ParsedMessage parsed = new ParsedMessage(readEmlFile(file, false));
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		System.out.println("to addresses = " + parsed.getToAddressesAsString());
		assertNotNull(parsed.getToAddressesAsString());
		
		String fromDatiCert = parsed.getMittenteAddressFromDatiCertPec();
		System.out.println("from dati cert = " + fromDatiCert);
		
		assertNotNull(fromDatiCert);
		assertEquals("studiodurand@legalmail.it", fromDatiCert);
		
		String html = parsed.getHtmlParts();
		System.out.println(html);
		assertNotNull(html);
		assertTrue(html.trim().startsWith("<HTML><HEAD><TITLE>POSTA CERTIFICATA: definizione TE4I SRL  08534231009</TITLE></HEAD>"));
	}
	
}
