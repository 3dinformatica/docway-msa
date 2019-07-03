package it.tredi.msa.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import it.tredi.msa.configuration.docway.AssegnatarioMailboxConfiguration;
import it.tredi.msa.configuration.docway.Docway4MailboxConfiguration;
import it.tredi.msa.mailboxmanager.DocWay4DummyMailboxManager;
import it.tredi.msa.mailboxmanager.DummyMailReader;
import it.tredi.msa.mailboxmanager.docway.DocTipoEnum;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.mailboxmanager.docway.FascicoloReference;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su estrazione contenuto di eml attraverso l'implementazione per DocWay4
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
//@Ignore // TODO per l'attivazione richiede un archivio eXtraWay opportunamente configurato
public class ArchiviazioneTagsTest extends EmlReader {
	
	private static final String TAGS_EML_LOCATION = "tags";
	
	private static final String COD_AMM = "3DIN";
	private static final String COD_AOO = "BOL";
	
	private static final String XW_HOST = "127.0.0.1";
	private static final int XW_PORT = 4859;
	private static final String XW_DOCWAY_DB = "xdocwaydoc";
	private static final String XW_ACL_DB = "acl";
	
	private static final String EXPECTED_COD_FASC = "2018-3DINBOL-01/02.00006.00002.00002";

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
		
		configuration.setArchiviazioneByTags(true);
		configuration.setAspettoClassificazione("AA/DD");
		
		configuration.setCodAmm(COD_AMM);
		configuration.setCodAmmInteropPA(COD_AMM);
		configuration.setCodAoo(COD_AOO);
		configuration.setCodAooInteropPA(COD_AOO);
		configuration.setCodAmmAoo(COD_AMM + COD_AOO);
		
		configuration.setOper("archiviatore automatico");
		configuration.setUffOper("unit-test");
		
		configuration.setTipoDoc("arrivo");
		
		AssegnatarioMailboxConfiguration responsabile = new AssegnatarioMailboxConfiguration();
		responsabile.setNomePersona("Iommi Thomas");
		responsabile.setCodPersona("PI000102");
		responsabile.setNomeUff("Servizio Tecnico Bologna");
		responsabile.setCodUff("00003");
		responsabile.setTipo("RPA");
		configuration.setResponsabile(responsabile);
		
		List<AssegnatarioMailboxConfiguration> ccs = new ArrayList<AssegnatarioMailboxConfiguration>(); 
		AssegnatarioMailboxConfiguration cc = new AssegnatarioMailboxConfiguration();
		cc.setNomePersona("Stagni Simone");
		cc.setCodPersona("PI000008");
		cc.setNomeUff("Servizio Tecnico Bologna");
		cc.setCodUff("00003");
		cc.setTipo("CC");
		cc.setIntervento(true);
		ccs.add(cc);
		configuration.setAssegnatariCC(ccs);
				
		configuration.setXwHost(XW_HOST);
		configuration.setXwPort(XW_PORT);
		configuration.setXwDb(XW_DOCWAY_DB);
		configuration.setAclDb(XW_ACL_DB);
		
		return configuration;
	}
	
	/**
	 * Validazione di un documento registrato come Varie
	 * @param document
	 */
	private void validateVarie(DocwayDocument document) {
		assertNotNull(document);
		assertEquals(DocTipoEnum.VARIE.getText(), document.getTipo());
		assertEquals(0, document.getRifEsterni().size());
		assertEquals("Mirko Bernardini", document.getAutore());
		
		validateRifIntFascicolato(document);
	}
	
	/**
	 * Validazione dei rif. interni (assegnatari del documento) in caso di fascicolazione tramite TAGS
	 * @param document
	 */
	private void validateRifIntFascicolato(DocwayDocument document) {
		assertEquals(3, document.getRifInterni().size());
		assertEquals(EXPECTED_COD_FASC, document.getRifInterni().get(0).getCodFasc());
		assertEquals("PI000008", document.getRifInterni().get(0).getCodPersona());
		assertEquals("RPA", document.getRifInterni().get(0).getDiritto());
		assertEquals(EXPECTED_COD_FASC, document.getRifInterni().get(1).getCodFasc());
		assertEquals("PI000060", document.getRifInterni().get(1).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(1).getDiritto());
		assertFalse(document.getRifInterni().get(1).isIntervento());
		assertNull(document.getRifInterni().get(2).getCodFasc());
		assertEquals("PI000102", document.getRifInterni().get(2).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(2).getDiritto());
		assertTrue(document.getRifInterni().get(2).isIntervento());
	}
	
	/**
	 * Test di archiviazione di un messaggio in arrivo fascicolato tramite TAGS
	 * @throws Exception
	 */
	@Test
	public void arrivoTags() throws Exception {
		String fileName = "arrivo-tags.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		assertEquals(0, parsed.getAttachments().size());
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#agrodolce", parsed.getSubjectTags().get(0));
		assertEquals("#2019", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNotNull(fascicolo);
		assertNotNull(fascicolo.getCodFascicolo());
		assertEquals(EXPECTED_COD_FASC, fascicolo.getCodFascicolo());
		assertNotNull(fascicolo.getOggetto());
		assertNotEquals("", fascicolo.getOggetto());
		
		System.out.println("fascicolo.numero = " + fascicolo.getCodFascicolo());
		System.out.println("fascicolo.oggetto = " + fascicolo.getOggetto());
		
		assertNotNull(fascicolo.getRifs());
		assertTrue(fascicolo.getRifs().size() > 0); // ogni fascicolo ha almeno un rif. interno (rpa)
		
		System.out.println("fascicolo.rifs.size = " + fascicolo.getRifs().size());
		
		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertFalse(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertFalse(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertFalse(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// definizione del flusso del documento
		assertEquals(DocTipoEnum.ARRIVO, this.mailboxManager.flussoByRecipients(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		assertEquals(1, document.getRifEsterni().size());
		assertNotNull(document.getRifEsterni().get(0).getCod());
		assertEquals("PE001574", document.getRifEsterni().get(0).getCod());
		validateRifIntFascicolato(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di archiviazione di un messaggio in partenza fascicolato tramite TAGS
	 * @throws Exception
	 */
	@Test
	public void partenzaTags() throws Exception {
		String fileName = "partenza-tags.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#agrodolce", parsed.getSubjectTags().get(0));
		assertEquals("#2019", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNotNull(fascicolo);
		assertNotNull(fascicolo.getCodFascicolo());
		assertEquals(EXPECTED_COD_FASC, fascicolo.getCodFascicolo());
		assertNotNull(fascicolo.getOggetto());
		assertNotEquals("", fascicolo.getOggetto());
		
		System.out.println("fascicolo.numero = " + fascicolo.getCodFascicolo());
		System.out.println("fascicolo.oggetto = " + fascicolo.getOggetto());
		
		assertNotNull(fascicolo.getRifs());
		assertTrue(fascicolo.getRifs().size() > 0); // ogni fascicolo ha almeno un rif. interno (rpa)
		
		System.out.println("fascicolo.rifs.size = " + fascicolo.getRifs().size());
		
		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertFalse(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertTrue(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio 
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertTrue(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// definizione del flusso del documento
		assertEquals(DocTipoEnum.PARTENZA, this.mailboxManager.flussoByRecipients(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.PARTENZA.getText(), document.getTipo());
		assertEquals(1, document.getRifEsterni().size());
		assertNull(document.getRifEsterni().get(0).getCod());
		assertEquals("mbernard78@googlemail.com", document.getRifEsterni().get(0).getEmail());
		validateRifIntFascicolato(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di archiviazion di un messaggio in contenente TAGS sconosciuti nell'oggetto (parsing classico del messaggio)
	 * @throws Exception
	 */
	@Test
	public void tagsSconosciuti() throws Exception {
		String fileName = "tags-sconosciuti.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#agrodole", parsed.getSubjectTags().get(0));
		assertEquals("#2019", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNull(fascicolo);

		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertFalse(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertTrue(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio 
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertTrue(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		assertEquals(1, document.getRifEsterni().size());
		assertNull(document.getRifEsterni().get(0).getCod());
		assertEquals("Mirko Bernardini", document.getRifEsterni().get(0).getNome());
		assertEquals("mbernardini@3di.it", document.getRifEsterni().get(0).getEmail());
		assertEquals(2, document.getRifInterni().size());
		assertNull(document.getRifInterni().get(0).getCodFasc());
		assertEquals("PI000102", document.getRifInterni().get(0).getCodPersona());
		assertEquals("RPA", document.getRifInterni().get(0).getDiritto());
		assertNull(document.getRifInterni().get(1).getCodFasc());
		assertEquals("PI000008", document.getRifInterni().get(1).getCodPersona());
		assertEquals("CC", document.getRifInterni().get(1).getDiritto());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di archiviazione TAGS come doc non protocollato (mittente e destinatari tutti interni)
	 * @throws Exception
	 */
	@Test
	public void mittenteDestinatriInterniTags() throws Exception {
		String fileName = "solo-interni-tags.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		assertEquals(0, parsed.getAttachments().size());
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#2019", parsed.getSubjectTags().get(0));
		assertEquals("#agrodolce", parsed.getSubjectTags().get(1));
				
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNotNull(fascicolo);
		assertNotNull(fascicolo.getCodFascicolo());
		assertEquals(EXPECTED_COD_FASC, fascicolo.getCodFascicolo());
		assertNotNull(fascicolo.getOggetto());
		assertNotEquals("", fascicolo.getOggetto());
		
		System.out.println("fascicolo.numero = " + fascicolo.getCodFascicolo());
		System.out.println("fascicolo.oggetto = " + fascicolo.getOggetto());
		
		assertNotNull(fascicolo.getRifs());
		assertTrue(fascicolo.getRifs().size() > 0); // ogni fascicolo ha almeno un rif. interno (rpa)
		
		System.out.println("fascicolo.rifs.size = " + fascicolo.getRifs().size());
		
		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertFalse(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertTrue(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio 
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertFalse(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// definizione del flusso del documento
		assertEquals(DocTipoEnum.VARIE, this.mailboxManager.flussoByRecipients(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		validateVarie(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di inoltro a casella di archiviazione TAGS
	 * @throws Exception
	 */
	@Test
	public void inoltroMailboxTags() throws Exception {
		String fileName = "inoltro-casella-tags.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#2019", parsed.getSubjectTags().get(0));
		assertEquals("#agrodolce", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNotNull(fascicolo);
		assertNotNull(fascicolo.getCodFascicolo());
		assertEquals(EXPECTED_COD_FASC, fascicolo.getCodFascicolo());
		assertNotNull(fascicolo.getOggetto());
		assertNotEquals("", fascicolo.getOggetto());
		
		System.out.println("fascicolo.numero = " + fascicolo.getCodFascicolo());
		System.out.println("fascicolo.oggetto = " + fascicolo.getOggetto());
		
		assertNotNull(fascicolo.getRifs());
		assertTrue(fascicolo.getRifs().size() > 0); // ogni fascicolo ha almeno un rif. interno (rpa)
		
		System.out.println("fascicolo.rifs.size = " + fascicolo.getRifs().size());
		
		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertTrue(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertTrue(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio 
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertFalse(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// definizione del flusso del documento
		assertEquals(DocTipoEnum.VARIE, this.mailboxManager.flussoByRecipients(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		validateVarie(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di archiviazione di un messaggio in partenza fascicolato tramite TAGS
	 * @throws Exception
	 */
	@Test
	public void partenzaTags2() throws Exception {
		String fileName = "partenza-tags-2.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
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
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#agrodolce", parsed.getSubjectTags().get(0));
		assertEquals("#2019", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNull(interopDocument); // non si tratta di un messaggio di interoperabilita'
		
		// recupero del fascicolo in base ai TAGS individuati sull'oggetto
		FascicoloReference fascicolo = this.mailboxManager.fascicoloByTags(COD_AMM + COD_AOO, parsed.getSubjectTags());
		assertNotNull(fascicolo);
		assertNotNull(fascicolo.getCodFascicolo());
		assertEquals(EXPECTED_COD_FASC, fascicolo.getCodFascicolo());
		assertNotNull(fascicolo.getOggetto());
		assertNotEquals("", fascicolo.getOggetto());
		
		System.out.println("fascicolo.numero = " + fascicolo.getCodFascicolo());
		System.out.println("fascicolo.oggetto = " + fascicolo.getOggetto());
		
		assertNotNull(fascicolo.getRifs());
		assertTrue(fascicolo.getRifs().size() > 0); // ogni fascicolo ha almeno un rif. interno (rpa)
		
		System.out.println("fascicolo.rifs.size = " + fascicolo.getRifs().size());
		
		// verifico se si tratta di un messaggio inoltrato alla casella di archiviazione TAGS
		assertFalse(this.mailboxManager.isForwardedToTagsMailbox(parsed));
		
		// controllo se il mittente del messaggio e' interno o esterno (doc in partenza o arrivo)
		assertTrue(this.mailboxManager.isMittenteRifInt(parsed));
		
		// verifico la presenza di rif interni fra i destinatari del messaggio 
		assertTrue(this.mailboxManager.containsDestinatariRifInt(parsed));
		
		// verifico la presenza di rif esterni fra i destinatari del messaggio
		assertTrue(this.mailboxManager.containsDestinatariRifEst(parsed));
		
		// definizione del flusso del documento
		assertEquals(DocTipoEnum.PARTENZA, this.mailboxManager.flussoByRecipients(parsed));
		
		// conversione da message a document
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.PARTENZA.getText(), document.getTipo());
		assertEquals(3, document.getRifEsterni().size());
		assertNull(document.getRifEsterni().get(0).getCod());
		assertEquals("Thomas Iommi", document.getRifEsterni().get(0).getNome());
		assertEquals("tiommi@3di.it", document.getRifEsterni().get(0).getEmail());
		assertNull(document.getRifEsterni().get(1).getCod());
		assertEquals("mbernard78@googlemail.com", document.getRifEsterni().get(1).getNome());
		assertEquals("mbernard78@googlemail.com", document.getRifEsterni().get(1).getEmail());
		assertNull(document.getRifEsterni().get(2).getCod());
		assertEquals("Marvin Pascale", document.getRifEsterni().get(2).getNome());
		assertEquals("pascale@jandm.it", document.getRifEsterni().get(2).getEmail());
		validateRifIntFascicolato(document);
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
}
