package it.tredi.msa.test.extraway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
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
import it.tredi.msa.mailboxmanager.DocWay4DummyMailboxManager;
import it.tredi.msa.mailboxmanager.DummyMailReader;
import it.tredi.msa.mailboxmanager.docway.DocTipoEnum;
import it.tredi.msa.mailboxmanager.docway.DocwayDocument;
import it.tredi.msa.mailboxmanager.docway.DocwayFile;
import it.tredi.msa.mailboxmanager.docway.DocwayParsedMessage;
import it.tredi.msa.test.EmlReader;
import it.tredi.msa.test.conf.MsaTesterApplication;

/**
 * UnitTest su estrazione estrazione allegati contenuti in un file zip allegato al messaggio da processare
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MsaTesterApplication.class)
public class ExtractZipTest extends EmlReader {
	
	private static final String ZIP_EML_LOCATION = "extract_zip";

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
	 * Test di archiviazione di un messaggio contenente un file ZIP da estrarre
	 * @throws Exception
	 */
	@Test
	public void arrivoTags() throws Exception {
		String fileName = "zip.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + ZIP_EML_LOCATION + "/" + fileName);
		
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
	 * Test di archiviazione di un messaggio contenente un file ZIP da estrarre
	 * @throws Exception
	 */
	@Test
	public void arrivo2Tags() throws Exception {
		String fileName = "zip2.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + ZIP_EML_LOCATION + "/" + fileName);
		
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
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed, false);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		
		// controllo su allegati estratti dal documento
		
		assertEquals(3, document.getAllegato().size());
		assertEquals("zippo.zip", document.getAllegato().get(2));
		
		assertNotNull(document.getFiles());
		for (DocwayFile dwfile : document.getFiles())
			if (!dwfile.getName().startsWith("testo email"))
				System.out.println("attach name (from zip) = " + dwfile.getName());
		assertEquals(5, document.getFiles().size());
		
		assertNotNull(document.getImmagini());
		for (DocwayFile dwfile : document.getImmagini())
			System.out.println("image name (from zip) = " + dwfile.getName());
		assertEquals(1, document.getImmagini().size());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
	/**
	 * Test di archiviazione di un messaggio di INTEROPERABILITA' contenente un file ZIP da estrarre
	 * @throws Exception
	 */
	@Test
	public void segnaturaTags() throws Exception {
		String fileName = "segnatura_con_zip.eml";
		File file = ResourceUtils.getFile("classpath:" + EML_LOCATION + "/" + ZIP_EML_LOCATION + "/" + fileName);
		
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
		DocwayDocument document = this.mailboxManager.buildDocwayDocument(parsed, false);
		assertNotNull(document);
		assertEquals(DocTipoEnum.ARRIVO.getText(), document.getTipo());
		
		// controllo su allegati estratti dal documento
		
		assertEquals(3, document.getAllegato().size());
		assertEquals("guida-di-angular-5.zip", document.getAllegato().get(2));
		
		assertNotNull(document.getFiles());
		for (DocwayFile dwfile : document.getFiles())
			if (!dwfile.getName().startsWith("testo email"))
				System.out.println("attach name (from zip) = " + dwfile.getName());
		assertEquals(2, document.getFiles().size());
		
		assertNotNull(document.getImmagini());
		for (DocwayFile dwfile : document.getImmagini())
			System.out.println("image name (from zip) = " + dwfile.getName());
		assertEquals(20, document.getImmagini().size());
		
		this.mailboxManager.processMessage(parsed); // chiamo il metodo di processMessage solo per verificare che non vengano restituite eccezioni
				
		assertEquals(0, parsed.getRelevantMssages().size());
	}
	
}
