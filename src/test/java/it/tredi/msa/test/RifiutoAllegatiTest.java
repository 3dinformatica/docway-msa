package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.configuration.docway.RifiutoByAttachmentsConfiguration;
import it.tredi.msa.mailboxmanager.DocWay4DummyMailboxManager;
import it.tredi.msa.mailboxmanager.DummyMailReader;
import it.tredi.msa.mailboxmanager.docway.DocTipoEnum;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.DocwayFile;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su rifiuto messaggi in base ad allegati non supportati dal sistema documentale
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
@ActiveProfiles({ "local" })
public class RifiutoAllegatiTest extends EmlReader {

	private static final String COD_AMM = "3DIN";
	private static final String COD_AOO = "BOL";
	
	private static final String XW_HOST = "127.0.0.1";
	private static final int XW_PORT = 4859;
	private static final String XW_DOCWAY_DB = "xdocwaydoc";
	private static final String XW_ACL_DB = "acl";
	
	/**
	 * Manager di test per DocWay4
	 */
	private DocWay4DummyMailboxManager mailboxManager;
	
	/**
	 * Inizializzazione del manager di test per DocWay4
	 * @throws Exception
	 */
	@Before
	public void initManager() throws Exception {
		this.mailboxManager = new DocWay4DummyMailboxManager();
		this.mailboxManager.setConfiguration(buildConfiguration());
		this.mailboxManager.setMailReader(new DummyMailReader());
		this.mailboxManager.openSession();
	}
	
	/**
	 * Chiusura del manager di test per DocWay4
	 * @throws Exception
	 */
	@After
	public void closeManager() throws Exception {
		this.mailboxManager.closeSession();
	}
	
	/**
	 * Costruzione della configurazione mailbox di test
	 * @return
	 */
	private Docway4MailboxConfiguration buildConfiguration() {
		Docway4MailboxConfiguration configuration = new Docway4MailboxConfiguration();
		configuration.setName("CONF-TEST");
		configuration.setAddress("test-archiviatore@libero.it");
		configuration.setPec(false);
		
		configuration.setCodAmm(COD_AMM);
		configuration.setCodAmmInteropPA(COD_AMM);
		configuration.setCodAoo(COD_AOO);
		configuration.setCodAooInteropPA(COD_AOO);
		configuration.setCodAmmAoo(COD_AMM + COD_AOO);
		
		configuration.setOper("archiviatore automatico");
		configuration.setUffOper("unit-test");
		
		configuration.setTipoDoc("arrivo");
		
		configuration.setExtractZip(true);
		
		RifiutoByAttachmentsConfiguration rifiutoByAttachments = new RifiutoByAttachmentsConfiguration();
		rifiutoByAttachments.setEnabled(true);
		rifiutoByAttachments.setAllowedExtensions(Arrays.asList(new String[] { "doc", "pdf", "txt" }));
		configuration.setRifiutoByAttachments(rifiutoByAttachments);
		
		AssegnatarioMailboxConfiguration responsabile = new AssegnatarioMailboxConfiguration();
		responsabile.setNomePersona("Iommi Thomas");
		responsabile.setCodPersona("PI000102");
		responsabile.setNomeUff("Servizio Tecnico Bologna");
		responsabile.setCodUff("00003");
		responsabile.setTipo("RPA");
		configuration.setResponsabile(responsabile);
		
		List<AssegnatarioMailboxConfiguration> ccs = new ArrayList<AssegnatarioMailboxConfiguration>(); 
		configuration.setAssegnatariCC(ccs);
				
		configuration.setXwHost(XW_HOST);
		configuration.setXwPort(XW_PORT);
		configuration.setXwDb(XW_DOCWAY_DB);
		configuration.setAclDb(XW_ACL_DB);
		
		return configuration;
	}
	
	/**
	 * Test di rifiuto con file ZIP su classica mail in arrivo
	 * @throws Exception
	 */
	@Test
	public void rifiutoZipArrivoTest() throws Exception {
		String fileName = "zip.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/extract_zip/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertFalse(parsed.isPecMessage());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(1, parsed.getAttachments().size());
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed, false);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		
		// controllo su stato di rifiuto
		assertTrue(document.isRifiutato());
		assertNotNull(document.getRifiuto().getMotivazione());
		System.out.println("RIFIUTO = " + document.getRifiuto().getMotivazione());
		
		assertNull(parsed.getMotivazioneNotificaEccezioneToSend());
		
		// controllo su allegati estratti dal documento
		
		assertEquals(1, document.getAllegato().size());
		assertEquals("test.zip", document.getAllegato().get(0));
		
		assertNotNull(document.getFiles());
		for (DocwayFile dwfile : document.getFiles())
			if (!dwfile.getName().startsWith("testo email"))
				System.out.println("attach name (from zip) = " + dwfile.getName());
		assertEquals(4, document.getFiles().size());
		
		assertNotNull(document.getImmagini());
		for (DocwayFile dwfile : document.getImmagini())
			System.out.println("image name (from zip) = " + dwfile.getName());
		assertEquals(1, document.getImmagini().size());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di rifiuto di un file ZIP su messaggio di INTEROPERABILITA'
	 * @throws Exception
	 */
	@Test
	public void rifiutoZipSegnaturaTest() throws Exception {
		String fileName = "segnatura_con_zip.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/extract_zip/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertFalse(parsed.isPecMessage());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocWayDocumentByInterop(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		
		// controllo su stato di rifiuto
		assertTrue(document.isRifiutato());
		assertNotNull(document.getRifiuto().getMotivazione());
		System.out.println("RIFIUTO = " + document.getRifiuto().getMotivazione());
		
		assertNotNull(parsed.getMotivazioneNotificaEccezioneToSend());
		String notificaEccezione = parsed.getMotivazioneNotificaEccezioneToSend();
		System.out.println("NOTIFICA ECCEZIONE = " + notificaEccezione);
		assertTrue(notificaEccezione.contains(document.getRifiuto().getMotivazione()));
		
		// controllo su allegati estratti dal documento
		
		assertNotNull(document.getFiles());
		for (DocwayFile dwfile : document.getFiles())
			if (!dwfile.getName().startsWith("testo email"))
				System.out.println("attach name (from zip) = " + dwfile.getName());
		assertEquals(1, document.getFiles().size());
		assertEquals("guidadiangular5.html", document.getFiles().get(0).getName());
		
		assertNotNull(document.getImmagini());
		for (DocwayFile dwfile : document.getImmagini())
			System.out.println("image name (from zip) = " + dwfile.getName());
		assertEquals(20, document.getImmagini().size());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
		
		// controllo su messaggio di notifica eccezione
		
		assertEquals(1, parsed.getRelevantMssages().size());
		assertEquals(notificaEccezione, parsed.getRelevantMssages().get(0));
	}
	
	// TODO TEST di MAIL ACCETTATA (NESSUN FILE NON AMMESSO INCLUSO) 
	
	
	
}
