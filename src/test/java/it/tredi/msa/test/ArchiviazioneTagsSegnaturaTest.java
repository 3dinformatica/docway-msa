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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles({ "local" })
public class ArchiviazioneTagsSegnaturaTest extends EmlReader {
	
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
		configuration.setName("CONF-PEC-TEST");
		configuration.setAddress("sviluppo@pec.3di.it");
		configuration.setPec(true);
		
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
	 * Test di archiviazion di un messaggio ricevuto in interoperabilita' e fascicolato tramite TAGS
	 * @throws Exception
	 */
	@Test
	public void arrivoTags() throws Exception {
		String fileName = "segnatura-tags.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + TAGS_EML_LOCATION + "/" + fileName);
		
		System.out.println("input file = " + fileName);
		
		DocwayParsedMessage parsed = new DocwayParsedMessage(readEmlFile(file), false);
		
		assertNotNull(parsed);
		assertNotNull(parsed.getMessageId());
		
		assertTrue(parsed.isPecMessage());
		assertFalse(parsed.isPecReceipt());
		
		System.out.println("messageId = " + parsed.getMessageId());
		System.out.println("subject = " + parsed.getSubject());
		System.out.println("from address = " + parsed.getFromAddress());
		
		List<String> attachments = parsed.getAttachmentsName();
		System.out.println("attachments count = " + attachments.size());
		for (String name : attachments)
			System.out.println("\tattach name = " + name);
		
		assertEquals(3, parsed.getAttachments().size());
		
		// verifica dei TAGS contenuti nell'oggetto del messaggio
		assertTrue(parsed.containsTags());
		assertEquals(2, parsed.getSubjectTags().size());
		assertEquals("#2019", parsed.getSubjectTags().get(0));
		assertEquals("#agrodolce", parsed.getSubjectTags().get(1));
		
		Document interopDocument = parsed.getSegnaturaInteropPADocument();
		assertNotNull(interopDocument);
		
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
		DocwayDocument document = this.mailboxManager.buildDocWayDocumentByInterop(parsed);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		assertEquals(1, document.getRifEsterni().size());
		assertNull(document.getRifEsterni().get(0).getCod());
		assertEquals("2018-COMUASO-0000954", document.getRifEsterni().get(0).getnProt());
		assertEquals("20180529", document.getRifEsterni().get(0).getDataProt());
		assertEquals("Comune di Asolo", document.getRifEsterni().get(0).getNome());
		assertEquals("assistenza@pec.3di.it", document.getRifEsterni().get(0).getEmailCertificata());
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
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
}
