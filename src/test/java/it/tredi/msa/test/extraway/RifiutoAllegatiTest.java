package it.tredi.msa.test.extraway;

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
import it.tredi.msa.test.EmlReader;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su rifiuto messaggi in base ad allegati non supportati dal sistema documentale
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class RifiutoAllegatiTest extends EmlReader {

	private static final String COD_AMM = "3DIN";
	private static final String COD_AOO = "BOL";
	
	private static final String XW_HOST = "127.0.0.1";
	private static final int XW_PORT = 4859;
	private static final String XW_DOCWAY_DB = "xdocwaydoc";
	private static final String XW_ACL_DB = "acl";
	
	private static final String COD_FASCICOLO_RIFIUTO = "2019-3DINBOL-01/02.00001";
	private static final List<String> ESTENSIONI_AMMESSE_RIFIUTO = Arrays.asList(new String[] { "doc", "pdf", "txt" });
	
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
		rifiutoByAttachments.setAllowedExtensions(ESTENSIONI_AMMESSE_RIFIUTO);
		rifiutoByAttachments.setCodFascicolo(COD_FASCICOLO_RIFIUTO);
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
		
		// controllo su fascicolazione da rifiuto
		this.checkFascicolazioneDocumentoDaRifiuto(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Validazione dei dati derivanti dalla fascicolazione del documento
	 * @param document
	 * @throws Exception
	 */
	private void checkFascicolazioneDocumentoDaRifiuto(DocwayDocument document) throws Exception {
		assertEquals(4, document.getRifInterni().size());
		assertEquals(COD_FASCICOLO_RIFIUTO, document.getRifInterni().get(0).getCodFasc());
		assertEquals("PI000008", document.getRifInterni().get(0).getCodPersona());
		assertEquals("RPA", document.getRifInterni().get(0).getDiritto());
		assertEquals(COD_FASCICOLO_RIFIUTO, document.getRifInterni().get(1).getCodFasc());
		assertEquals("tutti_SI000010", document.getRifInterni().get(1).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(1).getDiritto());
		assertFalse(document.getRifInterni().get(1).isIntervento());
		assertEquals(COD_FASCICOLO_RIFIUTO, document.getRifInterni().get(2).getCodFasc());
		assertEquals("PI000060", document.getRifInterni().get(2).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(2).getDiritto());
		assertFalse(document.getRifInterni().get(2).isIntervento());
		assertNull(document.getRifInterni().get(3).getCodFasc());
		assertEquals("PI000102", document.getRifInterni().get(3).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(3).getDiritto());
		assertTrue(document.getRifInterni().get(3).isIntervento());
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
		
		// controllo su fascicolazione da rifiuto
		this.checkFascicolazioneDocumentoDaRifiuto(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
		
		// controllo su messaggio di notifica eccezione		
		assertEquals(1, parsed.getRelevantMssages().size());
		assertEquals(notificaEccezione, parsed.getRelevantMssages().get(0));
	}
	
	/**
	 * Test di messaggio di posta accettato (nessun file non ammesso incluso)
	 * @throws Exception
	 */
	@Test
	public void arrivoAccettatoTest() throws Exception {
		String fileName = " java.lang.ClassCastException.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertTrue(parsed.isPecMessage());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed, false);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		
		// controllo su stato di rifiuto
		assertFalse(document.isRifiutato());
		assertNull(document.getRifiuto());
		
		// controllo su allegati estratti dal documento
		assertEquals(3, document.getAllegato().size());
		
		assertNotNull(document.getFiles());
		for (DocwayFile dwfile : document.getFiles())
			System.out.println("attach name = " + dwfile.getName());
		assertEquals(4, document.getFiles().size());
		
		assertNotNull(document.getImmagini());
		for (DocwayFile dwfile : document.getImmagini())
			System.out.println("image name = " + dwfile.getName());
		assertEquals(0, document.getImmagini().size());
		
		// controllo su fascicolazione da rifiuto NON eseguita
		assertEquals(1, document.getRifInterni().size());
		assertNull(document.getRifInterni().get(0).getCodFasc());
		assertEquals("PI000102", document.getRifInterni().get(0).getCodPersona());
		assertEquals("RPA", document.getRifInterni().get(0).getDiritto());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	
}
